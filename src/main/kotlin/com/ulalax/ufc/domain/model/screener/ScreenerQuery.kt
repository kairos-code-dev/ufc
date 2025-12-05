package com.ulalax.ufc.domain.model.screener

/**
 * Screener 검색 쿼리를 나타내는 추상 클래스
 *
 * 주식(Equity)과 펀드(Fund)에 따라 다른 필드를 사용하므로
 * EquityQuery와 FundQuery로 구분됩니다.
 */
sealed class ScreenerQuery {
    abstract val operator: ScreenerOperator
    abstract val operands: List<Any>

    /**
     * 쿼리를 Yahoo API 요청 형식으로 변환
     */
    abstract fun toRequestBody(): Map<String, Any>

    /**
     * 쿼리 유효성 검사
     * @throws IllegalArgumentException 유효하지 않은 쿼리인 경우
     */
    abstract fun validate()

    /**
     * 쿼리 타입 (EQUITY 또는 MUTUALFUND)
     */
    abstract val quoteType: String
}

/**
 * 주식(Equity) 검색 쿼리
 */
class EquityQuery(
    override val operator: ScreenerOperator,
    override val operands: List<Any>
) : ScreenerQuery() {

    override val quoteType: String = "EQUITY"

    companion object {
        /**
         * AND 조건 생성
         */
        fun and(vararg queries: EquityQuery): EquityQuery {
            return EquityQuery(ScreenerOperator.AND, queries.toList())
        }

        /**
         * OR 조건 생성
         */
        fun or(vararg queries: EquityQuery): EquityQuery {
            return EquityQuery(ScreenerOperator.OR, queries.toList())
        }

        /**
         * 같음 조건 생성
         */
        fun eq(field: EquityField, value: Any): EquityQuery {
            return EquityQuery(ScreenerOperator.EQ, listOf(field.apiValue, value))
        }

        /**
         * 초과 조건 생성
         */
        fun gt(field: EquityField, value: Number): EquityQuery {
            return EquityQuery(ScreenerOperator.GT, listOf(field.apiValue, value))
        }

        /**
         * 미만 조건 생성
         */
        fun lt(field: EquityField, value: Number): EquityQuery {
            return EquityQuery(ScreenerOperator.LT, listOf(field.apiValue, value))
        }

        /**
         * 이상 조건 생성
         */
        fun gte(field: EquityField, value: Number): EquityQuery {
            return EquityQuery(ScreenerOperator.GTE, listOf(field.apiValue, value))
        }

        /**
         * 이하 조건 생성
         */
        fun lte(field: EquityField, value: Number): EquityQuery {
            return EquityQuery(ScreenerOperator.LTE, listOf(field.apiValue, value))
        }

        /**
         * 범위 조건 생성
         */
        fun between(field: EquityField, min: Number, max: Number): EquityQuery {
            return EquityQuery(ScreenerOperator.BTWN, listOf(field.apiValue, min, max))
        }

        /**
         * 포함 조건 생성 (OR + EQ로 변환됨)
         */
        fun isIn(field: EquityField, vararg values: Any): EquityQuery {
            require(values.isNotEmpty()) { "isIn requires at least one value" }
            if (values.size == 1) {
                return eq(field, values[0])
            }
            return or(*values.map { eq(field, it) }.toTypedArray())
        }
    }

    override fun toRequestBody(): Map<String, Any> {
        return when (operator) {
            ScreenerOperator.AND, ScreenerOperator.OR -> {
                mapOf(
                    "operator" to operator.apiValue,
                    "operands" to operands.map { (it as ScreenerQuery).toRequestBody() }
                )
            }
            ScreenerOperator.IS_IN -> {
                // IS_IN should have been converted to OR + EQ in isIn() factory method
                throw IllegalStateException("IS_IN should be converted to OR + EQ before toRequestBody()")
            }
            else -> {
                mapOf(
                    "operator" to operator.apiValue,
                    "operands" to operands
                )
            }
        }
    }

    override fun validate() {
        when (operator) {
            ScreenerOperator.AND, ScreenerOperator.OR -> {
                require(operands.size >= 2) { "${operator.apiValue} operator requires at least 2 operands" }
                require(operands.all { it is ScreenerQuery }) { "${operator.apiValue} operands must be ScreenerQuery" }
                operands.forEach { (it as ScreenerQuery).validate() }
            }
            ScreenerOperator.EQ -> {
                require(operands.size == 2) { "EQ operator requires exactly 2 operands" }
                require(operands[0] is String) { "First operand must be field name (String)" }
            }
            ScreenerOperator.GT, ScreenerOperator.LT, ScreenerOperator.GTE, ScreenerOperator.LTE -> {
                require(operands.size == 2) { "${operator.apiValue} operator requires exactly 2 operands" }
                require(operands[0] is String) { "First operand must be field name (String)" }
                require(operands[1] is Number) { "Second operand must be Number for ${operator.apiValue}" }
            }
            ScreenerOperator.BTWN -> {
                require(operands.size == 3) { "BTWN operator requires exactly 3 operands" }
                require(operands[0] is String) { "First operand must be field name (String)" }
                require(operands[1] is Number) { "Second operand must be Number (min)" }
                require(operands[2] is Number) { "Third operand must be Number (max)" }
            }
            ScreenerOperator.IS_IN -> {
                throw IllegalStateException("IS_IN should be converted to OR + EQ before validate()")
            }
        }
    }
}

/**
 * 펀드(Fund) 검색 쿼리
 */
class FundQuery(
    override val operator: ScreenerOperator,
    override val operands: List<Any>
) : ScreenerQuery() {

    override val quoteType: String = "MUTUALFUND"

    companion object {
        /**
         * AND 조건 생성
         */
        fun and(vararg queries: FundQuery): FundQuery {
            return FundQuery(ScreenerOperator.AND, queries.toList())
        }

        /**
         * OR 조건 생성
         */
        fun or(vararg queries: FundQuery): FundQuery {
            return FundQuery(ScreenerOperator.OR, queries.toList())
        }

        /**
         * 같음 조건 생성
         */
        fun eq(field: FundField, value: Any): FundQuery {
            return FundQuery(ScreenerOperator.EQ, listOf(field.apiValue, value))
        }

        /**
         * 초과 조건 생성
         */
        fun gt(field: FundField, value: Number): FundQuery {
            return FundQuery(ScreenerOperator.GT, listOf(field.apiValue, value))
        }

        /**
         * 미만 조건 생성
         */
        fun lt(field: FundField, value: Number): FundQuery {
            return FundQuery(ScreenerOperator.LT, listOf(field.apiValue, value))
        }

        /**
         * 이상 조건 생성
         */
        fun gte(field: FundField, value: Number): FundQuery {
            return FundQuery(ScreenerOperator.GTE, listOf(field.apiValue, value))
        }

        /**
         * 이하 조건 생성
         */
        fun lte(field: FundField, value: Number): FundQuery {
            return FundQuery(ScreenerOperator.LTE, listOf(field.apiValue, value))
        }

        /**
         * 범위 조건 생성
         */
        fun between(field: FundField, min: Number, max: Number): FundQuery {
            return FundQuery(ScreenerOperator.BTWN, listOf(field.apiValue, min, max))
        }

        /**
         * 포함 조건 생성 (OR + EQ로 변환됨)
         */
        fun isIn(field: FundField, vararg values: Any): FundQuery {
            require(values.isNotEmpty()) { "isIn requires at least one value" }
            if (values.size == 1) {
                return eq(field, values[0])
            }
            return or(*values.map { eq(field, it) }.toTypedArray())
        }
    }

    override fun toRequestBody(): Map<String, Any> {
        return when (operator) {
            ScreenerOperator.AND, ScreenerOperator.OR -> {
                mapOf(
                    "operator" to operator.apiValue,
                    "operands" to operands.map { (it as ScreenerQuery).toRequestBody() }
                )
            }
            ScreenerOperator.IS_IN -> {
                throw IllegalStateException("IS_IN should be converted to OR + EQ before toRequestBody()")
            }
            else -> {
                mapOf(
                    "operator" to operator.apiValue,
                    "operands" to operands
                )
            }
        }
    }

    override fun validate() {
        when (operator) {
            ScreenerOperator.AND, ScreenerOperator.OR -> {
                require(operands.size >= 2) { "${operator.apiValue} operator requires at least 2 operands" }
                require(operands.all { it is ScreenerQuery }) { "${operator.apiValue} operands must be ScreenerQuery" }
                operands.forEach { (it as ScreenerQuery).validate() }
            }
            ScreenerOperator.EQ -> {
                require(operands.size == 2) { "EQ operator requires exactly 2 operands" }
                require(operands[0] is String) { "First operand must be field name (String)" }
            }
            ScreenerOperator.GT, ScreenerOperator.LT, ScreenerOperator.GTE, ScreenerOperator.LTE -> {
                require(operands.size == 2) { "${operator.apiValue} operator requires exactly 2 operands" }
                require(operands[0] is String) { "First operand must be field name (String)" }
                require(operands[1] is Number) { "Second operand must be Number for ${operator.apiValue}" }
            }
            ScreenerOperator.BTWN -> {
                require(operands.size == 3) { "BTWN operator requires exactly 3 operands" }
                require(operands[0] is String) { "First operand must be field name (String)" }
                require(operands[1] is Number) { "Second operand must be Number (min)" }
                require(operands[2] is Number) { "Third operand must be Number (max)" }
            }
            ScreenerOperator.IS_IN -> {
                throw IllegalStateException("IS_IN should be converted to OR + EQ before validate()")
            }
        }
    }
}
