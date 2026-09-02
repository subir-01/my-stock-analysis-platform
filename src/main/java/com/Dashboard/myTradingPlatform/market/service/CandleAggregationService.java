package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CandleAggregationService {

    private static final String I1 = "I1";
    private static final String I5 = "I5";
    private static final String I15 = "I15";

    private static final int I5_INTERVAL = 5;
    private static final int I15_INTERVAL = 15;

    /*
     * =========================================================
     * CURRENT I5 BUCKETS
     * =========================================================
     *
     * instrument
     *     ↓
     * bucket start
     *     ↓
     * I1 timestamp -> I1 candle
     *
     * Example:
     *
     * instrument
     *   09:15
     *      09:15 -> candle
     *      09:16 -> candle
     *      09:17 -> candle
     *      09:18 -> candle
     *      09:19 -> candle
     */

    private final Map<String, CandleBucket> i5Buckets =
            new ConcurrentHashMap<>();


    /*
     * =========================================================
     * CURRENT I15 BUCKETS
     * =========================================================
     */

    private final Map<String, CandleBucket> i15Buckets =
            new ConcurrentHashMap<>();


    /*
     * =========================================================
     * PROCESS I1
     * =========================================================
     */

    public AggregationResult processI1Candle(
            MarketCandle candle) {

        if (!isValid(candle)) {
            return AggregationResult.empty();
        }

        if (!I1.equalsIgnoreCase(
                candle.timeframe())) {

            log.debug(
                    "Ignoring non-I1 candle: instrument={}, timeframe={}",
                    candle.instrumentKey(),
                    candle.timeframe()
            );

            return AggregationResult.empty();
        }

        /*
         * Process I5.
         */
        MarketCandle completedI5 =
                processBucket(
                        candle,
                        I5,
                        I5_INTERVAL,
                        i5Buckets
                );

        /*
         * Process I15.
         */
        MarketCandle completedI15 =
                processBucket(
                        candle,
                        I15,
                        I15_INTERVAL,
                        i15Buckets
                );

        return new AggregationResult(
                completedI5,
                completedI15
        );
    }


    /*
     * =========================================================
     * PROCESS BUCKET
     * =========================================================
     */

    private MarketCandle processBucket(
            MarketCandle incoming,
            String timeframe,
            int intervalMinutes,
            Map<String, CandleBucket> buckets) {

        String instrumentKey =
                incoming.instrumentKey();

        Instant bucketStart =
                calculateBucketStart(
                        incoming.timestamp(),
                        intervalMinutes
                );

        CandleBucket current =
                buckets.get(instrumentKey);


        /*
         * =====================================================
         * FIRST BUCKET
         * =====================================================
         */

        if (current == null) {

            CandleBucket newBucket =
                    new CandleBucket(
                            bucketStart
                    );

            /*
             * Store I1 by timestamp.
             *
             * If the same I1 timestamp arrives again,
             * it will REPLACE the previous snapshot.
             */
            newBucket.candles.put(
                    incoming.timestamp(),
                    incoming
            );

            buckets.put(
                    instrumentKey,
                    newBucket
            );

            log.debug(
                    "Started {} bucket: instrument={}, timestamp={}",
                    timeframe,
                    instrumentKey,
                    bucketStart
            );

            return null;
        }


        /*
         * =====================================================
         * SAME BUCKET
         * =====================================================
         */

        if (current.bucketStart.equals(bucketStart)) {

            /*
             * IMPORTANT:
             *
             * put() replaces an existing I1 candle having
             * the same timestamp.
             *
             * Therefore repeated WebSocket updates for:
             *
             * 09:17
             *
             * do NOT double-count volume.
             */
            current.candles.put(
                    incoming.timestamp(),
                    incoming
            );

            log.debug(
                    "Updated {} bucket: instrument={}, bucket={}, I1 timestamp={}",
                    timeframe,
                    instrumentKey,
                    bucketStart,
                    incoming.timestamp()
            );

            return null;
        }


        /*
         * =====================================================
         * NEW BUCKET
         * =====================================================
         */

        if (bucketStart.isAfter(
                current.bucketStart)) {

            /*
             * Build the completed candle from all I1 candles
             * stored in the previous bucket.
             */
            MarketCandle completed =
                    buildAggregatedCandle(
                            instrumentKey,
                            timeframe,
                            current
                    );

            /*
             * Create the new bucket.
             */
            CandleBucket newBucket =
                    new CandleBucket(
                            bucketStart
                    );

            newBucket.candles.put(
                    incoming.timestamp(),
                    incoming
            );

            buckets.put(
                    instrumentKey,
                    newBucket
            );

            log.info(
                    "{} bucket completed: instrument={}, timestamp={}, OHLCV={} / {} / {} / {} / {}",
                    timeframe,
                    instrumentKey,
                    completed.timestamp(),
                    completed.open(),
                    completed.high(),
                    completed.low(),
                    completed.close(),
                    completed.volume()
            );

            return completed;
        }


        /*
         * =====================================================
         * OUT OF ORDER CANDLE
         * =====================================================
         */

        log.debug(
                "Ignoring out-of-order {} candle: instrument={}, incomingBucket={}, currentBucket={}",
                timeframe,
                instrumentKey,
                bucketStart,
                current.bucketStart
        );

        return null;
    }


    /*
     * =========================================================
     * BUILD AGGREGATED CANDLE
     * =========================================================
     */

    private MarketCandle buildAggregatedCandle(
            String instrumentKey,
            String timeframe,
            CandleBucket bucket) {

        if (bucket.candles.isEmpty()) {
            return null;
        }

        /*
         * TreeMap guarantees chronological ordering.
         */
        MarketCandle first =
                bucket.candles
                        .firstEntry()
                        .getValue();

        MarketCandle last =
                bucket.candles
                        .lastEntry()
                        .getValue();


        BigDecimal high = null;

        BigDecimal low = null;

        Long volume = 0L;


        /*
         * =====================================================
         * CALCULATE HIGH / LOW / VOLUME
         * =====================================================
         */

        for (MarketCandle candle :
                bucket.candles.values()) {

            /*
             * HIGH
             */
            if (candle.high() != null) {

                if (high == null
                        || candle.high().compareTo(high) > 0) {

                    high = candle.high();
                }
            }


            /*
             * LOW
             */
            if (candle.low() != null) {

                if (low == null
                        || candle.low().compareTo(low) < 0) {

                    low = candle.low();
                }
            }


            /*
             * VOLUME
             *
             * MarketCandle.volume() is Long.
             */
            if (candle.volume() != null) {

                volume =
                        volume + candle.volume();
            }
        }


        /*
         * =====================================================
         * CREATE FINAL CANDLE
         * =====================================================
         *
         * OPEN  = first I1 open
         * HIGH  = highest I1 high
         * LOW   = lowest I1 low
         * CLOSE = last I1 close
         * VOLUME = sum of I1 volumes
         */

        return new MarketCandle(

                instrumentKey,

                timeframe,

                first.open(),

                high,

                low,

                last.close(),

                volume,

                bucket.bucketStart
        );
    }


    /*
     * =========================================================
     * CALCULATE BUCKET START
     * =========================================================
     */

    private Instant calculateBucketStart(
            Instant timestamp,
            int intervalMinutes) {

        ZonedDateTime dateTime =
                timestamp.atZone(
                        ZoneOffset.UTC
                );

        int minute =
                dateTime.getMinute();

        int bucketMinute =
                (minute / intervalMinutes)
                        * intervalMinutes;

        return dateTime
                .withMinute(bucketMinute)
                .withSecond(0)
                .withNano(0)
                .toInstant();
    }


    /*
     * =========================================================
     * GET CURRENT I5
     * =========================================================
     *
     * This returns the CURRENT FORMING I5 candle.
     */

    public MarketCandle getCurrentI5(
            String instrumentKey) {

        CandleBucket bucket =
                i5Buckets.get(
                        instrumentKey
                );

        if (bucket == null) {
            return null;
        }

        return buildAggregatedCandle(
                instrumentKey,
                I5,
                bucket
        );
    }


    /*
     * =========================================================
     * GET CURRENT I15
     * =========================================================
     */

    public MarketCandle getCurrentI15(
            String instrumentKey) {

        CandleBucket bucket =
                i15Buckets.get(
                        instrumentKey
                );

        if (bucket == null) {
            return null;
        }

        return buildAggregatedCandle(
                instrumentKey,
                I15,
                bucket
        );
    }


    /*
     * =========================================================
     * GET CURRENT BUCKET I1 COUNT
     * =========================================================
     */

    public int getCurrentI5CandleCount(
            String instrumentKey) {

        CandleBucket bucket =
                i5Buckets.get(
                        instrumentKey
                );

        if (bucket == null) {
            return 0;
        }

        return bucket.candles.size();
    }


    public int getCurrentI15CandleCount(
            String instrumentKey) {

        CandleBucket bucket =
                i15Buckets.get(
                        instrumentKey
                );

        if (bucket == null) {
            return 0;
        }

        return bucket.candles.size();
    }


    /*
     * =========================================================
     * REMOVE INSTRUMENT
     * =========================================================
     */

    public void removeInstrument(
            String instrumentKey) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            return;
        }

        i5Buckets.remove(
                instrumentKey
        );

        i15Buckets.remove(
                instrumentKey
        );

        log.debug(
                "Removed aggregation state: instrument={}",
                instrumentKey
        );
    }


    /*
     * =========================================================
     * CLEAR
     * =========================================================
     */

    public void clear() {

        i5Buckets.clear();

        i15Buckets.clear();

        log.info(
                "All candle aggregation state cleared"
        );
    }


    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    private boolean isValid(
            MarketCandle candle) {

        return candle != null

                && candle.instrumentKey() != null
                && !candle.instrumentKey().isBlank()

                && candle.timeframe() != null
                && !candle.timeframe().isBlank()

                && candle.timestamp() != null

                && candle.open() != null
                && candle.high() != null
                && candle.low() != null
                && candle.close() != null;
    }


    /*
     * =========================================================
     * BUCKET
     * =========================================================
     */

    private static class CandleBucket {

        private final Instant bucketStart;

        /*
         * Timestamp → I1 candle
         *
         * TreeMap keeps candles sorted by timestamp.
         */
        private final TreeMap<Instant, MarketCandle> candles =
                new TreeMap<>();


        private CandleBucket(
                Instant bucketStart) {

            this.bucketStart =
                    bucketStart;
        }
    }


    /*
     * =========================================================
     * RESULT
     * =========================================================
     */

    public record AggregationResult(
            MarketCandle completedI5,
            MarketCandle completedI15) {


        public static AggregationResult empty() {

            return new AggregationResult(
                    null,
                    null
            );
        }
    }
}