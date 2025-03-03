/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.indexmanagement.rollup.model

import org.opensearch.action.search.SearchResponse
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.common.xcontent.ToXContent
import org.opensearch.common.xcontent.ToXContentObject
import org.opensearch.common.xcontent.XContentBuilder
import org.opensearch.common.xcontent.XContentParser
import org.opensearch.common.xcontent.XContentParser.Token
import org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken
import org.opensearch.index.seqno.SequenceNumbers
import org.opensearch.indexmanagement.indexstatemanagement.util.WITH_TYPE
import org.opensearch.indexmanagement.opensearchapi.instant
import org.opensearch.indexmanagement.util.NO_ID
import org.opensearch.search.aggregations.bucket.composite.InternalComposite
import java.io.IOException
import java.time.Instant
import java.util.Locale

data class ContinuousMetadata(
    val nextWindowStartTime: Instant,
    val nextWindowEndTime: Instant
) : ToXContentObject, Writeable {

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        nextWindowStartTime = sin.readInstant(),
        nextWindowEndTime = sin.readInstant()
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        return builder.startObject()
            .timeField(NEXT_WINDOW_START_TIME_FIELD, NEXT_WINDOW_START_TIME_FIELD_IN_MILLIS, nextWindowStartTime.toEpochMilli())
            .timeField(NEXT_WINDOW_END_TIME_FIELD, NEXT_WINDOW_END_TIME_FIELD_IN_MILLIS, nextWindowEndTime.toEpochMilli())
            .endObject()
    }

    override fun writeTo(out: StreamOutput) {
        out.writeInstant(nextWindowStartTime)
        out.writeInstant(nextWindowEndTime)
    }

    companion object {
        private const val NEXT_WINDOW_START_TIME_FIELD = "next_window_start_time"
        private const val NEXT_WINDOW_START_TIME_FIELD_IN_MILLIS = "next_window_start_time_in_millis"
        private const val NEXT_WINDOW_END_TIME_FIELD = "next_window_end_time"
        private const val NEXT_WINDOW_END_TIME_FIELD_IN_MILLIS = "next_window_end_time_in_millis"

        @Suppress("ComplexMethod", "LongMethod")
        @JvmStatic
        @Throws(IOException::class)
        fun parse(xcp: XContentParser): ContinuousMetadata {
            var windowStartTime: Instant? = null
            var windowEndTime: Instant? = null

            ensureExpectedToken(Token.START_OBJECT, xcp.currentToken(), xcp)
            while (xcp.nextToken() != Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()

                when (fieldName) {
                    NEXT_WINDOW_START_TIME_FIELD -> windowStartTime = xcp.instant()
                    NEXT_WINDOW_END_TIME_FIELD -> windowEndTime = xcp.instant()
                }
            }

            return ContinuousMetadata(
                nextWindowStartTime = requireNotNull(windowStartTime) { "Next window start time must not be null for a continuous job" },
                nextWindowEndTime = requireNotNull(windowEndTime) { "Next window end time must not be null for a continuous job" }
            )
        }
    }
}

data class RollupStats(
    val pagesProcessed: Long,
    val documentsProcessed: Long,
    val rollupsIndexed: Long,
    val indexTimeInMillis: Long,
    val searchTimeInMillis: Long
) : ToXContentObject, Writeable {

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        pagesProcessed = sin.readLong(),
        documentsProcessed = sin.readLong(),
        rollupsIndexed = sin.readLong(),
        indexTimeInMillis = sin.readLong(),
        searchTimeInMillis = sin.readLong()
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        return builder.startObject()
            .field(PAGES_PROCESSED_FIELD, pagesProcessed)
            .field(DOCUMENTS_PROCESSED_FIELD, documentsProcessed)
            .field(ROLLUPS_INDEXED_FIELD, rollupsIndexed)
            .field(INDEX_TIME_IN_MILLIS_FIELD, indexTimeInMillis)
            .field(SEARCH_TIME_IN_MILLIS_FIELD, searchTimeInMillis)
            .endObject()
    }

    override fun writeTo(out: StreamOutput) {
        out.writeLong(pagesProcessed)
        out.writeLong(documentsProcessed)
        out.writeLong(rollupsIndexed)
        out.writeLong(indexTimeInMillis)
        out.writeLong(searchTimeInMillis)
    }

    companion object {
        private const val PAGES_PROCESSED_FIELD = "pages_processed" // The number of pages processed (paginations)
        private const val DOCUMENTS_PROCESSED_FIELD = "documents_processed" // The number of raw documents processed
        private const val ROLLUPS_INDEXED_FIELD = "rollups_indexed" // The number of rollup documents indexed
        private const val INDEX_TIME_IN_MILLIS_FIELD = "index_time_in_millis" // The total time spent indexing rollup documents
        private const val SEARCH_TIME_IN_MILLIS_FIELD = "search_time_in_millis" // The total time spent querying/aggregating live documents

        @Suppress("ComplexMethod", "LongMethod")
        @JvmStatic
        @Throws(IOException::class)
        fun parse(xcp: XContentParser): RollupStats {
            var pagesProcessed: Long? = null
            var documentsProcessed: Long? = null
            var rollupsIndexed: Long? = null
            var indexTimeInMillis: Long? = null
            var searchTimeInMillis: Long? = null

            ensureExpectedToken(Token.START_OBJECT, xcp.currentToken(), xcp)
            while (xcp.nextToken() != Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()

                when (fieldName) {
                    PAGES_PROCESSED_FIELD -> pagesProcessed = xcp.longValue()
                    DOCUMENTS_PROCESSED_FIELD -> documentsProcessed = xcp.longValue()
                    ROLLUPS_INDEXED_FIELD -> rollupsIndexed = xcp.longValue()
                    INDEX_TIME_IN_MILLIS_FIELD -> indexTimeInMillis = xcp.longValue()
                    SEARCH_TIME_IN_MILLIS_FIELD -> searchTimeInMillis = xcp.longValue()
                }
            }

            return RollupStats(
                pagesProcessed = requireNotNull(pagesProcessed) { "Pages processed must not be null" },
                documentsProcessed = requireNotNull(documentsProcessed) { "Documents processed must not be null" },
                rollupsIndexed = requireNotNull(rollupsIndexed) { "Rollups indexed must not be null" },
                indexTimeInMillis = requireNotNull(indexTimeInMillis) { "Index time in millis must not be null" },
                searchTimeInMillis = requireNotNull(searchTimeInMillis) { "Search time in millis must not be null" }
            )
        }
    }
}

data class RollupMetadata(
    val id: String = NO_ID,
    val seqNo: Long = SequenceNumbers.UNASSIGNED_SEQ_NO,
    val primaryTerm: Long = SequenceNumbers.UNASSIGNED_PRIMARY_TERM,
    val rollupID: String,
    val afterKey: Map<String, Any>? = null,
    val lastUpdatedTime: Instant,
    val continuous: ContinuousMetadata? = null,
    val status: Status,
    val failureReason: String? = null,
    val stats: RollupStats
) : ToXContentObject, Writeable {

    enum class Status(val type: String) {
        INIT("init"),
        STARTED("started"),
        STOPPED("stopped"),
        FINISHED("finished"),
        FAILED("failed"),
        RETRY("retry");

        override fun toString(): String {
            return type
        }
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        id = sin.readString(),
        seqNo = sin.readLong(),
        primaryTerm = sin.readLong(),
        rollupID = sin.readString(),
        afterKey = if (sin.readBoolean()) sin.readMap() else null,
        lastUpdatedTime = sin.readInstant(),
        continuous = if (sin.readBoolean()) ContinuousMetadata(sin) else null,
        status = sin.readEnum(Status::class.java),
        failureReason = sin.readOptionalString(),
        stats = RollupStats(sin)
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.startObject()
        if (params.paramAsBoolean(WITH_TYPE, false)) builder.startObject(ROLLUP_METADATA_TYPE)

        builder.field(ROLLUP_ID_FIELD, rollupID)
        if (afterKey != null) builder.field(AFTER_KEY_FIELD, afterKey)
        builder.timeField(LAST_UPDATED_FIELD, LAST_UPDATED_FIELD_IN_MILLIS, lastUpdatedTime.toEpochMilli())
        if (continuous != null) builder.field(CONTINUOUS_FIELD, continuous)
        builder.field(STATUS_FIELD, status.type)
        builder.field(FAILURE_REASON, failureReason)
        builder.field(STATS_FIELD, stats)

        if (params.paramAsBoolean(WITH_TYPE, false)) builder.endObject()
        return builder.endObject()
    }

    override fun writeTo(out: StreamOutput) {
        out.writeString(id)
        out.writeLong(seqNo)
        out.writeLong(primaryTerm)
        out.writeString(rollupID)
        out.writeBoolean(afterKey != null)
        afterKey?.let { out.writeMap(it) }
        out.writeInstant(lastUpdatedTime)
        out.writeBoolean(continuous != null)
        continuous?.writeTo(out)
        out.writeEnum(status)
        out.writeOptionalString(failureReason)
        stats.writeTo(out)
    }

    companion object {
        const val ROLLUP_METADATA_TYPE = "rollup_metadata"
        const val ROLLUP_ID_FIELD = "rollup_id"
        const val AFTER_KEY_FIELD = "after_key"
        const val LAST_UPDATED_FIELD = "last_updated_time"
        const val LAST_UPDATED_FIELD_IN_MILLIS = "last_updated_time_in_millis"
        const val CONTINUOUS_FIELD = "continuous"
        const val STATUS_FIELD = "status"
        const val FAILURE_REASON = "failure_reason"
        const val STATS_FIELD = "stats"

        @Suppress("ComplexMethod", "LongMethod")
        @JvmStatic
        @Throws(IOException::class)
        fun parse(
            xcp: XContentParser,
            id: String = NO_ID,
            seqNo: Long = SequenceNumbers.UNASSIGNED_SEQ_NO,
            primaryTerm: Long = SequenceNumbers.UNASSIGNED_PRIMARY_TERM
        ): RollupMetadata {
            var rollupID: String? = null
            var afterKey: Map<String, Any>? = null
            var lastUpdatedTime: Instant? = null
            var continuous: ContinuousMetadata? = null
            var status: Status? = null
            var failureReason: String? = null
            var stats: RollupStats? = null

            ensureExpectedToken(Token.START_OBJECT, xcp.currentToken(), xcp)
            while (xcp.nextToken() != Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()

                when (fieldName) {
                    ROLLUP_ID_FIELD -> rollupID = xcp.text()
                    AFTER_KEY_FIELD -> afterKey = xcp.map()
                    LAST_UPDATED_FIELD -> lastUpdatedTime = xcp.instant()
                    CONTINUOUS_FIELD -> continuous = ContinuousMetadata.parse(xcp)
                    STATUS_FIELD -> status = Status.valueOf(xcp.text().uppercase(Locale.ROOT))
                    FAILURE_REASON -> failureReason = xcp.textOrNull()
                    STATS_FIELD -> stats = RollupStats.parse(xcp)
                }
            }

            return RollupMetadata(
                id,
                seqNo,
                primaryTerm,
                rollupID = requireNotNull(rollupID) { "RollupID must not be null" },
                afterKey = afterKey,
                lastUpdatedTime = requireNotNull(lastUpdatedTime) { "Last updated time must not be null" },
                continuous = continuous,
                status = requireNotNull(status) { "Status must not be null" },
                failureReason = failureReason,
                stats = requireNotNull(stats) { "Stats must not be null" }
            )
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readFrom(sin: StreamInput) = RollupMetadata(sin)
    }
}

fun RollupMetadata.incrementStats(response: SearchResponse, internalComposite: InternalComposite): RollupMetadata {
    return this.copy(
        stats = this.stats.copy(
            pagesProcessed = stats.pagesProcessed + 1L,
            documentsProcessed = stats.documentsProcessed +
                internalComposite.buckets.fold(0L) { acc, internalBucket -> acc + internalBucket.docCount },
            searchTimeInMillis = stats.searchTimeInMillis + response.took.millis
        )
    )
}

fun RollupMetadata.mergeStats(stats: RollupStats): RollupMetadata {
    return this.copy(
        stats = this.stats.copy(
            pagesProcessed = this.stats.pagesProcessed + stats.pagesProcessed,
            documentsProcessed = this.stats.documentsProcessed + stats.documentsProcessed,
            rollupsIndexed = this.stats.rollupsIndexed + stats.rollupsIndexed,
            indexTimeInMillis = this.stats.indexTimeInMillis + stats.indexTimeInMillis,
            searchTimeInMillis = this.stats.searchTimeInMillis + stats.searchTimeInMillis
        )
    )
}
