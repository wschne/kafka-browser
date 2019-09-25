package com.rewe.digital.gui.controls.helper.autocomplete

import spock.lang.Specification
import spock.lang.Unroll

class CalculateCaretPositionSpec extends Specification {
    def calculateCaretPosition = new CalculateCaretPosition()

    @Unroll
    def "Calculate new position #expectedPosition for an input-string #value"() {
        when:
        def result = calculateCaretPosition.forSelectedEntry(initialPosition, value)

        then:
        result == expectedPosition

        where:
        value                                                 | initialPosition | expectedPosition
        'select value from topic'                             | 9               | 12
        'select value.payload from topic'                     | 11              | 20
        'select value.payload from topic'                     | 11              | 20
        'select value.payload from topic where value.payload' | 46              | 51
    }
}
