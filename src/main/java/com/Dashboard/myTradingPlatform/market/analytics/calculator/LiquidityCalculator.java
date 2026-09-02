package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.LiquidityAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.LiquidityZone;
import com.Dashboard.myTradingPlatform.market.analytics.model.LiquidityZoneType;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class LiquidityCalculator {

    private static final int DEFAULT_LOOKBACK = 200;

    /*
     * Number of candles on each side used to identify
     * a swing high / swing low.
     */
    private static final int SWING_LOOKBACK = 2;

    /*
     * Nearby price points within this percentage are
     * considered part of the same liquidity area.
     *
     * Used internally only.
     */
    private static final BigDecimal CLUSTER_TOLERANCE =
            new BigDecimal("0.0010");

    /*
     * Price distance used to determine whether a candle
     * meaningfully interacted with a liquidity level.
     */
    private static final BigDecimal TOUCH_TOLERANCE =
            new BigDecimal("0.0015");

    /*
     * We don't want insignificant historical levels.
     */
    private static final int MIN_LIQUIDITY_SCORE = 25;

    /*
     * Maximum number of levels returned on each side.
     */
    private static final int MAX_SUPPORTS = 3;

    private static final int MAX_RESISTANCES = 3;

    public LiquidityAnalysis calculate(
            List<MarketCandle> inputCandles) {

        if (inputCandles == null
                || inputCandles.size() < 10) {

            return new LiquidityAnalysis(
                    null,
                    List.of(),
                    List.of()
            );
        }

        /*
         * ---------------------------------------------------------
         * CLEAN AND SORT CANDLES
         * ---------------------------------------------------------
         */

        List<MarketCandle> candles =
                inputCandles.stream()
                        .filter(this::isValidCandle)
                        .sorted(
                                Comparator.comparing(
                                        MarketCandle::timestamp
                                )
                        )
                        .toList();

        if (candles.size() < 10) {

            return new LiquidityAnalysis(
                    null,
                    List.of(),
                    List.of()
            );
        }

        /*
         * ---------------------------------------------------------
         * USE MOST RECENT LOOKBACK
         * ---------------------------------------------------------
         */

        if (candles.size() > DEFAULT_LOOKBACK) {

            candles =
                    new ArrayList<>(
                            candles.subList(
                                    candles.size()
                                            - DEFAULT_LOOKBACK,
                                    candles.size()
                            )
                    );
        }

        /*
         * ---------------------------------------------------------
         * CURRENT MARKET PRICE
         * ---------------------------------------------------------
         */

        MarketCandle latest =
                candles.get(candles.size() - 1);

        BigDecimal currentPrice =
                latest.close();

        if (currentPrice == null
                || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {

            return new LiquidityAnalysis(
                    null,
                    List.of(),
                    List.of()
            );
        }

        /*
         * ---------------------------------------------------------
         * FIND SWING POINTS
         * ---------------------------------------------------------
         */

        List<PricePoint> swingHighs =
                findSwingHighs(candles);

        List<PricePoint> swingLows =
                findSwingLows(candles);

        /*
         * ---------------------------------------------------------
         * CREATE CANDIDATE PRICE POINTS
         * ---------------------------------------------------------
         */

        List<PricePoint> supportCandidates =
                swingLows.stream()
                        .filter(point ->
                                point.price()
                                        .compareTo(currentPrice) < 0)
                        .toList();

        List<PricePoint> resistanceCandidates =
                swingHighs.stream()
                        .filter(point ->
                                point.price()
                                        .compareTo(currentPrice) > 0)
                        .toList();

        /*
         * ---------------------------------------------------------
         * CLUSTER NEARBY LEVELS
         * ---------------------------------------------------------
         */

        List<LiquidityCluster> supportClusters =
                clusterPricePoints(
                        supportCandidates
                );

        List<LiquidityCluster> resistanceClusters =
                clusterPricePoints(
                        resistanceCandidates
                );

        /*
         * ---------------------------------------------------------
         * BUILD SUPPORT LEVELS
         * ---------------------------------------------------------
         */

        List<LiquidityZone> supports =
                buildZones(
                        supportClusters,
                        candles,
                        currentPrice,
                        LiquidityZoneType.SUPPORT
                );

        /*
         * ---------------------------------------------------------
         * BUILD RESISTANCE LEVELS
         * ---------------------------------------------------------
         */

        List<LiquidityZone> resistances =
                buildZones(
                        resistanceClusters,
                        candles,
                        currentPrice,
                        LiquidityZoneType.RESISTANCE
                );

        /*
         * ---------------------------------------------------------
         * REMOVE WEAK LEVELS
         * ---------------------------------------------------------
         */

        supports =
                supports.stream()
                        .filter(zone ->
                                zone.liquidityScore()
                                        >= MIN_LIQUIDITY_SCORE)
                        .toList();

        resistances =
                resistances.stream()
                        .filter(zone ->
                                zone.liquidityScore()
                                        >= MIN_LIQUIDITY_SCORE)
                        .toList();

        /*
         * ---------------------------------------------------------
         * RANK LEVELS
         *
         * We want strong + nearby levels.
         * ---------------------------------------------------------
         */

        supports =
                rankZones(
                        supports,
                        currentPrice,
                        true
                );

        resistances =
                rankZones(
                        resistances,
                        currentPrice,
                        false
                );

        /*
         * ---------------------------------------------------------
         * RETURN ONLY TOP 3
         * ---------------------------------------------------------
         */

        supports =
                supports.stream()
                        .limit(MAX_SUPPORTS)
                        .toList();

        resistances =
                resistances.stream()
                        .limit(MAX_RESISTANCES)
                        .toList();

        return new LiquidityAnalysis(
                currentPrice,
                supports,
                resistances
        );
    }

    /*
     * =========================================================
     * SWING HIGH
     * =========================================================
     */

    private List<PricePoint> findSwingHighs(
            List<MarketCandle> candles) {

        List<PricePoint> result =
                new ArrayList<>();

        for (int i = SWING_LOOKBACK;
             i < candles.size() - SWING_LOOKBACK;
             i++) {

            MarketCandle current =
                    candles.get(i);

            boolean isSwingHigh = true;

            for (int j = 1;
                 j <= SWING_LOOKBACK;
                 j++) {

                if (current.high()
                        .compareTo(
                                candles.get(i - j).high()
                        ) <= 0) {

                    isSwingHigh = false;
                    break;
                }

                if (current.high()
                        .compareTo(
                                candles.get(i + j).high()
                        ) <= 0) {

                    isSwingHigh = false;
                    break;
                }
            }

            if (isSwingHigh) {

                result.add(
                        new PricePoint(
                                current.high(),
                                current.timestamp()
                        )
                );
            }
        }

        return result;
    }

    /*
     * =========================================================
     * SWING LOW
     * =========================================================
     */

    private List<PricePoint> findSwingLows(
            List<MarketCandle> candles) {

        List<PricePoint> result =
                new ArrayList<>();

        for (int i = SWING_LOOKBACK;
             i < candles.size() - SWING_LOOKBACK;
             i++) {

            MarketCandle current =
                    candles.get(i);

            boolean isSwingLow = true;

            for (int j = 1;
                 j <= SWING_LOOKBACK;
                 j++) {

                if (current.low()
                        .compareTo(
                                candles.get(i - j).low()
                        ) >= 0) {

                    isSwingLow = false;
                    break;
                }

                if (current.low()
                        .compareTo(
                                candles.get(i + j).low()
                        ) >= 0) {

                    isSwingLow = false;
                    break;
                }
            }

            if (isSwingLow) {

                result.add(
                        new PricePoint(
                                current.low(),
                                current.timestamp()
                        )
                );
            }
        }

        return result;
    }

    /*
     * =========================================================
     * CLUSTER PRICE POINTS
     * =========================================================
     */

    private List<LiquidityCluster> clusterPricePoints(
            List<PricePoint> points) {

        if (points.isEmpty()) {
            return List.of();
        }

        List<PricePoint> sorted =
                points.stream()
                        .sorted(
                                Comparator.comparing(
                                        PricePoint::price
                                )
                        )
                        .toList();

        List<LiquidityCluster> clusters =
                new ArrayList<>();

        List<PricePoint> currentCluster =
                new ArrayList<>();

        for (PricePoint point : sorted) {

            if (currentCluster.isEmpty()) {

                currentCluster.add(point);
                continue;
            }

            BigDecimal clusterAverage =
                    averagePrice(currentCluster);

            BigDecimal distance =
                    point.price()
                            .subtract(clusterAverage)
                            .abs()
                            .divide(
                                    clusterAverage,
                                    8,
                                    RoundingMode.HALF_UP
                            );

            if (distance.compareTo(
                    CLUSTER_TOLERANCE
            ) <= 0) {

                currentCluster.add(point);

            } else {

                clusters.add(
                        new LiquidityCluster(
                                new ArrayList<>(
                                        currentCluster
                                )
                        )
                );

                currentCluster.clear();
                currentCluster.add(point);
            }
        }

        if (!currentCluster.isEmpty()) {

            clusters.add(
                    new LiquidityCluster(
                            new ArrayList<>(
                                    currentCluster
                            )
                    )
            );
        }

        return clusters;
    }

    /*
     * =========================================================
     * BUILD ZONES
     * =========================================================
     */

    private List<LiquidityZone> buildZones(
            List<LiquidityCluster> clusters,
            List<MarketCandle> candles,
            BigDecimal currentPrice,
            LiquidityZoneType type) {

        List<LiquidityZone> zones =
                new ArrayList<>();

        for (LiquidityCluster cluster :
                clusters) {

            BigDecimal level =
                    averagePrice(
                            cluster.points()
                    );

            int touches =
                    countTouches(
                            level,
                            candles
                    );

            int rejections =
                    countRejections(
                            level,
                            candles,
                            type
                    );

            int swingPoints =
                    cluster.points().size();

            int liquidityScore =
                    calculateLiquidityScore(
                            touches,
                            rejections,
                            swingPoints,
                            cluster.points(),
                            candles
                    );

            int distancePoints =
                    calculateDistancePoints(
                            currentPrice,
                            level
                    );

            Instant firstSeen =
                    cluster.points().stream()
                            .map(PricePoint::timestamp)
                            .min(Instant::compareTo)
                            .orElse(null);

            Instant lastSeen =
                    cluster.points().stream()
                            .map(PricePoint::timestamp)
                            .max(Instant::compareTo)
                            .orElse(null);

            zones.add(
                    new LiquidityZone(
                            level.setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            ),
                            distancePoints,
                            liquidityScore,
                            touches,
                            rejections,
                            swingPoints,
                            type,
                            firstSeen,
                            lastSeen
                    )
            );
        }

        return zones;
    }

    /*
     * =========================================================
     * LIQUIDITY SCORE
     * =========================================================
     */

    private int calculateLiquidityScore(
            int touches,
            int rejections,
            int swingPoints,
            List<PricePoint> points,
            List<MarketCandle> candles) {

        /*
         * Touch component: maximum 30
         */
        int touchScore =
                Math.min(
                        touches * 3,
                        30
                );

        /*
         * Rejection component: maximum 30
         */
        int rejectionScore =
                Math.min(
                        rejections * 4,
                        30
                );

        /*
         * Swing component: maximum 20
         */
        int swingScore =
                Math.min(
                        swingPoints * 5,
                        20
                );

        /*
         * Recency component: maximum 20
         */
        int recencyScore =
                calculateRecencyScore(
                        points,
                        candles
                );

        return Math.min(
                touchScore
                        + rejectionScore
                        + swingScore
                        + recencyScore,
                100
        );
    }

    /*
     * =========================================================
     * RECENCY SCORE
     * =========================================================
     */

    private int calculateRecencyScore(
            List<PricePoint> points,
            List<MarketCandle> candles) {

        if (points.isEmpty()
                || candles.isEmpty()) {

            return 0;
        }

        Instant latestCandle =
                candles.get(candles.size() - 1)
                        .timestamp();

        Instant latestPoint =
                points.stream()
                        .map(PricePoint::timestamp)
                        .max(Instant::compareTo)
                        .orElse(null);

        if (latestPoint == null) {
            return 0;
        }

        long totalSeconds =
                Math.max(
                        1,
                        latestCandle.getEpochSecond()
                                - candles.get(0)
                                .timestamp()
                                .getEpochSecond()
                );

        long ageSeconds =
                Math.max(
                        0,
                        latestCandle.getEpochSecond()
                                - latestPoint
                                .getEpochSecond()
                );

        double ageRatio =
                (double) ageSeconds
                        / totalSeconds;

        if (ageRatio <= 0.20) {
            return 20;
        }

        if (ageRatio <= 0.40) {
            return 15;
        }

        if (ageRatio <= 0.60) {
            return 10;
        }

        if (ageRatio <= 0.80) {
            return 5;
        }

        return 2;
    }

    /*
     * =========================================================
     * TOUCH COUNT
     * =========================================================
     */

    private int countTouches(
            BigDecimal level,
            List<MarketCandle> candles) {

        int count = 0;

        for (MarketCandle candle : candles) {

            if (isNearLevel(
                    candle.low(),
                    level
            )
                    || isNearLevel(
                    candle.high(),
                    level
            )
                    || isNearLevel(
                    candle.close(),
                    level
            )) {

                count++;
            }
        }

        return count;
    }

    /*
     * =========================================================
     * REJECTION COUNT
     * =========================================================
     */

    private int countRejections(
            BigDecimal level,
            List<MarketCandle> candles,
            LiquidityZoneType type) {

        int count = 0;

        for (MarketCandle candle : candles) {

            BigDecimal range =
                    candle.high()
                            .subtract(candle.low());

            if (range.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal upperWick =
                    candle.high()
                            .subtract(
                                    candle.open()
                                            .max(candle.close())
                            );

            BigDecimal lowerWick =
                    candle.open()
                            .min(candle.close())
                            .subtract(
                                    candle.low()
                            );

            if (type
                    == LiquidityZoneType.RESISTANCE) {

                if (isNearLevel(
                        candle.high(),
                        level
                )
                        && upperWick
                        .compareTo(
                                range.multiply(
                                        new BigDecimal("0.30")
                                )
                        ) >= 0) {

                    count++;
                }

            } else {

                if (isNearLevel(
                        candle.low(),
                        level
                )
                        && lowerWick
                        .compareTo(
                                range.multiply(
                                        new BigDecimal("0.30")
                                )
                        ) >= 0) {

                    count++;
                }
            }
        }

        return count;
    }

    /*
     * =========================================================
     * RANK ZONES
     * =========================================================
     */

    private List<LiquidityZone> rankZones(
            List<LiquidityZone> zones,
            BigDecimal currentPrice,
            boolean support) {

        return zones.stream()
                .sorted(
                        Comparator
                                .comparingInt(
                                        (LiquidityZone zone) ->
                                                calculateRelevanceScore(
                                                        zone,
                                                        currentPrice
                                                )
                                )
                                .reversed()
                                .thenComparingInt(
                                        LiquidityZone::distancePoints
                                )
                )
                .toList();
    }

    /*
     * =========================================================
     * RELEVANCE SCORE
     * =========================================================
     *
     * Strong liquidity + close distance
     * gets the highest priority.
     * =========================================================
     */

    private int calculateRelevanceScore(
            LiquidityZone zone,
            BigDecimal currentPrice) {

        int liquidity =
                zone.liquidityScore();

        int distance =
                zone.distancePoints();

        /*
         * Distance score.
         *
         * 0-25 points  -> 40
         * 26-50        -> 32
         * 51-100       -> 24
         * 101-200      -> 16
         * 201-400      -> 8
         * >400         -> 2
         */
        int distanceScore;

        if (distance <= 25) {
            distanceScore = 40;

        } else if (distance <= 50) {
            distanceScore = 32;

        } else if (distance <= 100) {
            distanceScore = 24;

        } else if (distance <= 200) {
            distanceScore = 16;

        } else if (distance <= 400) {
            distanceScore = 8;

        } else {
            distanceScore = 2;
        }

        return liquidity + distanceScore;
    }

    /*
     * =========================================================
     * DISTANCE IN POINTS
     * =========================================================
     */

    private int calculateDistancePoints(
            BigDecimal currentPrice,
            BigDecimal level) {

        return currentPrice
                .subtract(level)
                .abs()
                .setScale(
                        0,
                        RoundingMode.HALF_UP
                )
                .intValue();
    }

    /*
     * =========================================================
     * PRICE DISTANCE
     * =========================================================
     */

    private boolean isNearLevel(
            BigDecimal price,
            BigDecimal level) {

        if (price == null
                || level == null
                || price.compareTo(BigDecimal.ZERO) <= 0) {

            return false;
        }

        BigDecimal distance =
                price.subtract(level)
                        .abs()
                        .divide(
                                level,
                                8,
                                RoundingMode.HALF_UP
                        );

        return distance.compareTo(
                TOUCH_TOLERANCE
        ) <= 0;
    }

    /*
     * =========================================================
     * AVERAGE PRICE
     * =========================================================
     */

    private BigDecimal averagePrice(
            List<PricePoint> points) {

        BigDecimal total =
                points.stream()
                        .map(PricePoint::price)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return total.divide(
                BigDecimal.valueOf(
                        points.size()
                ),
                8,
                RoundingMode.HALF_UP
        );
    }

    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    private boolean isValidCandle(
            MarketCandle candle) {

        return candle != null
                && candle.open() != null
                && candle.high() != null
                && candle.low() != null
                && candle.close() != null
                && candle.timestamp() != null
                && candle.high()
                .compareTo(candle.low()) >= 0;
    }

    /*
     * =========================================================
     * INTERNAL RECORDS
     * =========================================================
     */

    private record PricePoint(
            BigDecimal price,
            Instant timestamp
    ) {
    }

    private record LiquidityCluster(
            List<PricePoint> points
    ) {
    }
}