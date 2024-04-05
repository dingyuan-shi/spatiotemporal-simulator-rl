package org.sdy;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;

public class OrderLoader {
    private final Path rootPath;
    private int currentSecond;
    private int pointer = 0;
    private int bufferSize = 5000;
    private int date;
    private ArrayList<Order> orders;
    private CSVReader reader;
    private static final Order metaOrder = new Order("", 0, 0, 0, 0, 0, 0, 0, null);

    public OrderLoader(int date, Path rootPath) {
        this.rootPath = rootPath;
        this.currentSecond = 0;
        this.pointer = 0;
        this.date = date;
        this.initOrders();
    }

    private void initOrders() {
        String fileName = "order_with_cancel" + this.date + ".csv";
        String filePath = Paths.get(this.rootPath.toString(), fileName).toString();
        System.out.println(filePath);
        try {
            this.reader = new CSVReader(new FileReader(filePath));
            this.orders = new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadOrders();
    }

    private void loadOrders() {
        this.orders.clear();
        int size = 0;
        String[] line;
        while (true) {
            try {
                if (!((line = reader.readNext()) != null)) break;
                double[] cancelProb = new double[10];
                for(int i = 0; i < 10; i++) {
                    cancelProb[i] = Double.parseDouble(line[8 + i]);
                }
                Order order = metaOrder.clone();
                order.setAll(line[0], Integer.parseInt(line[1]), Integer.parseInt(line[2]), Double.parseDouble(line[3]), Double.parseDouble(line[4]), Double.parseDouble(line[5]), Double.parseDouble(line[6]), Double.parseDouble(line[7]), cancelProb);
                this.orders.add(order);
                size++;
                if (size % this.bufferSize == 0) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void skipTo(int second) {
        this.currentSecond = second;
        while (pointer < orders.size() && orders.get(pointer).getStartSecond() < second) {
            pointer++;
        }
        if (pointer == orders.size()) {
            loadOrders();
            pointer = 0;
        }
    }

    public List<Order> getOrders(int currentSecond) {
        ArrayList<Order> batchOrders = new ArrayList<>();
        while (pointer < orders.size() && orders.get(pointer).getStartSecond() < currentSecond) {
            batchOrders.add(orders.get(pointer++));
        }
        if (pointer == orders.size()) {
            loadOrders();
            pointer = 0;
        }
        this.currentSecond = currentSecond;
        return batchOrders;
    }

    public List<Order> getOrders() {
        return this.getOrders(this.currentSecond + 2);
    }

    public static void main(String[] args) {
        new OrderLoader(20161106, GlobalSetting.orderRoot);
        return;
    }
}
