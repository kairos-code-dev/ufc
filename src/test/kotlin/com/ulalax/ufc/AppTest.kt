package com.ulalax.ufc

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag

@Tag("unit")
@DisplayName("App 테스트")
internal class AppTest {

    @Test
    @DisplayName("앱이 정상적으로 시작되어야 한다")
    fun testAppInitialization() {
        // Phase 1에서 구현될 테스트
        assertThat(true).isTrue()
    }
}
