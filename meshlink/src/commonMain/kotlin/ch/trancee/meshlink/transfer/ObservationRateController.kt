package ch.trancee.meshlink.transfer

public class ObservationRateController(
    private val windowSize: Int = DEFAULT_WINDOW_SIZE,
) {
    private val acknowledgementIntervalsMillis: MutableList<Long> = mutableListOf()
    private var lastAcknowledgementTimestampMillis: Long? = null

    init {
        require(windowSize > 0) {
            "ObservationRateController windowSize must be greater than 0."
        }
    }

    public fun recordAcknowledgement(timestampMillis: Long): Unit {
        require(timestampMillis >= 0) {
            "ObservationRateController timestampMillis must be greater than or equal to 0."
        }

        val previousTimestampMillis: Long? = lastAcknowledgementTimestampMillis
        if (previousTimestampMillis != null) {
            val intervalMillis: Long = timestampMillis - previousTimestampMillis
            require(intervalMillis >= 0) {
                "ObservationRateController timestamps must be non-decreasing."
            }
            if (acknowledgementIntervalsMillis.size == windowSize) {
                acknowledgementIntervalsMillis.removeAt(index = 0)
            }
            acknowledgementIntervalsMillis += intervalMillis
        }
        lastAcknowledgementTimestampMillis = timestampMillis
    }

    public fun recommendedDelayMillis(): Long {
        if (acknowledgementIntervalsMillis.isEmpty()) {
            return 0L
        }
        return acknowledgementIntervalsMillis.average().toLong()
    }

    public companion object {
        public const val DEFAULT_WINDOW_SIZE: Int = 4
    }
}
