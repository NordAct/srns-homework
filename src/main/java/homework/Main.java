package homework;

import org.jzy3d.analysis.AWTAbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartFactory;
import org.jzy3d.chart.factories.IChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.painters.Font;
import org.jzy3d.plot3d.builder.SurfaceBuilder;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {
    //пути к файлам
    final static String FILE = "/Yvar14.txt";
    final static String L1OF = "/DK_LxOF.txt";

    //исходные данные
    final static double SD = 0.06; //СКО сигнала
    final static double SD_NOISE = 1; //СКО шума
    final static long FD = 33000000; //частота дискретизации
    final static long F0 = 8000000; //промежуточная частота
    final static double T = 0.001; //интервал наблюдений (в секундах)
    final static double TD = 1d / FD; //интервал дескритезации
    final static int T_COUNT = 511; //кол-во символов в ДК
    final static double TC = T / T_COUNT; //длительность 1 символа ДК

    //для графика
    final static int F_SEARCH_WIDTH = 2000 * 2; //ширина области поиска частоты Доплера
    final static int F_POINTS = 9; //кол-во точек по оси частот (возможные значения доплеровской частоты)
    final static int T_POINTS = 1022; //кол-во точек по оси времени
    final static double T_PER_POINT = T * 1000000d / T_POINTS; //переводим в мкс, т.к. программа не выносит маленькости велечины в системе СИ
    final static double F_PER_POINT = (double) F_SEARCH_WIDTH / F_POINTS;
    final static double[] F_VALUES = new double[F_POINTS]; //возможные значения доплеровской частоты (заполняются в ходе выполнения)
    final static double[] T_VALUES = new double[T_POINTS]; //возможные значения задержки (заполняются в ходе выполнения)

    //порог обнаружения
    final static double H = 1; //отношение априорных вероятностей
    final static double TE = T; //эффективная длительность сигнала
    final static double QE = (TE * SD*SD) / (2 * SD_NOISE*SD_NOISE * TD); //отношение сигнал/шум
    final static double HE = Math.log((1 + QE) * H) * ((TE * SD_NOISE*SD_NOISE * (1 + QE)) / (QE * TD)); //порог обнаружения

    public static void main(String[] args) {
        long processingStartTime = System.nanoTime();
        System.out.println("Please wait, this might take a while");
        //Чтение файлов
        List<Integer> input = processFile(FILE);
        List<Integer> l1of = processFile(L1OF);

        //Обработка
        double[][] values = new double[T_POINTS][F_POINTS];
        int overThreshold = 0; //счетчик превышения порога

        for (int i = 0; i < T_POINTS; i++) T_VALUES[i] = i * T_PER_POINT;
        for (int i = 0; i < F_POINTS; i++) F_VALUES[i] = (i - (int) (F_POINTS / 2)) * F_PER_POINT;

        for (int i = 0; i < T_POINTS; i++) {
            for (int j = 0; j < F_POINTS; j++) {
                double iVal = 0;
                double qVal = 0;
                for (int k = 0; k < input.size(); k++) {
                    int pos = (int) ((k * TD + T_VALUES[i] / 1000000d) / TC) % T_COUNT;
                    double cos = l1of.get(pos) * Math.cos(2 * Math.PI * (F0 + F_VALUES[j]) * k * TD);
                    double sin = l1of.get(pos) * Math.sin(2 * Math.PI * (F0 + F_VALUES[j]) * k * TD);

                    iVal += sin * input.get(k);
                    qVal += cos * input.get(k);
                }
                values[i][j] = iVal * iVal + qVal * qVal;
                if (values[i][j] >= HE) overThreshold++;
            }
        }
        long plotDisplayStartTime = System.nanoTime();
        System.out.printf("Processing was completed in %s ms\n", (System.nanoTime() - processingStartTime) / 1000000d);

        Plot plot = new Plot(values);
        try {
            AnalysisLauncher.open(plot);
        } catch (Exception e) {
            System.out.println("Failed to open plot graph");
            throw new RuntimeException(e);
        }
        System.out.printf("Plot window was created in %s ms\n", (System.nanoTime() - plotDisplayStartTime) / 1000000d);

        double max = Double.MIN_VALUE;
        int xMax = 0;
        int yMax = 0;
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[0].length; j++) {
                if (max < values[i][j]) {
                    max = values[i][j];
                    xMax = i;
                    yMax = j;
                }
            }
        }

        System.out.println("------------------------------------");
        System.out.printf("Max correlation value: %s\n", max);

        System.out.printf("Delay value: %s ms\n", T_VALUES[xMax] / 1000d);
        System.out.printf("Doppler value: %s Hz\n", F_VALUES[yMax]);

        System.out.printf("Over threshold values amount: %s\n", overThreshold);
        System.out.printf("False alarm chance: %s \n", (double) overThreshold/(T_POINTS * F_POINTS));
        System.out.println("------------------------------------");
    }

    private static List<Integer> processFile(String file) {
        InputStream inputStream = Main.class.getResourceAsStream(file);
        if (inputStream == null) {
            System.out.printf("Failed to load file \"%s\"%n", file);
            return List.of();
        }
        InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        List<Integer> inputData = new ArrayList<>();
        try {
            for (String line; (line = reader.readLine()) != null;) {
                line = line.replaceAll("[^a-zA-Z0-9-]", "");
                int val = Integer.parseInt(line);
                inputData.add(val);
            }
        } catch (IOException e) {
            System.out.printf("%s: failed to read line%n", e.getMessage());
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            System.out.printf("%s: failed to process file \"%s\"%n", e.getMessage(), file);
        }
        return inputData;
    }

    private static class Plot extends AWTAbstractAnalysis {
        final double[][] values;
        private Plot(double[][] values) {
            this.values = values;
        }

        @Override
        public void init(){
            //Задаем координаты
            List<Coord3d> coords = new ArrayList<>();
            for (int i = 0; i < T_POINTS; i++) {
                for (int j = 0; j < F_POINTS; j++) {
                    coords.add(new Coord3d(T_VALUES[i] , F_VALUES[j], values[i][j]));
                }
            }

            //Задаем настройки отрисовки
            final Shape surface = new SurfaceBuilder().delaunay(coords);
            surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface));
            surface.setFaceDisplayed(true);
            surface.setPolygonOffsetFillEnable(true);
            surface.setWireframeDisplayed(true);
            surface.setWireframeWidth(0.05f);
            surface.setWireframeColor(Color.BLACK);

            //Рисуем график
            IChartFactory f = new AWTChartFactory();
            chart = f.newChart(Quality.Nicest().setAlphaActivated(false)); //отключаем прозрачность, т.к. она сломана
            //поддерживаются только символы из англ. раскладки
            chart.getAxisLayout().setXAxisLabel("t, us");
            chart.getAxisLayout().setYAxisLabel("fd, Hz");
            chart.getAxisLayout().setZAxisLabel("Parrots");
            chart.getAxisLayout().setFont(Font.Helvetica_18);
            chart.getScene().getGraph().add(surface);
        }
    }
}