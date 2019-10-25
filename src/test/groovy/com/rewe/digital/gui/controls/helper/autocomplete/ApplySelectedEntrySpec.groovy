package com.rewe.digital.gui.controls.helper.autocomplete

import spock.lang.Specification
import spock.lang.Unroll

class ApplySelectedEntrySpec extends Specification {
    SqlQueryAnalyzer sqlQueryAnalyzer = Mock()

    def applySelectedEntry = new ApplySelectedEntry(sqlQueryAnalyzer)

    @Unroll
    def "Apply selected field #selectedEntry to the previously entered query '#initialQuery'"() {

        when:
        def result = applySelectedEntry.toQuery(selectedEntry, caretPosition, initialQuery)

        then:
        result == expectedResult

        and:
        _ * sqlQueryAnalyzer.isLastTypedCharacterADot(initialQuery, caretPosition) >> isCaretAtDot
        _ * sqlQueryAnalyzer.getWordAtPosition(initialQuery, caretPosition) >> selectedWords

        where:
        initialQuery                                | selectedWords               | selectedEntry | caretPosition | isCaretAtDot | expectedResult
        'select value. from topic'                  | ['value']                   | 'payload'     | 13            | true         | 'select value.payload from topic'
        'select val from topic'                     | ['val']                     | 'value'       | 10            | false        | 'select value from topic'
        'select  from topic'                        | []                          | 'value'       | 7             | false        | 'select value from topic'
        'select value.payload.tim from topic'       | ['value', 'payload', 'tim'] | 'timestamp'   | 24            | false        | 'select value.payload.timestamp from topic'
        'select value from topic where value.'      | ['value']                   | 'payload'     | 36            | true         | 'select value from topic where value.payload'
        'select value.key from topic where value.k' | ['value', 'k']              | 'key'         | 41            | false        | 'select value.key from topic where value.key'
        'select value.prop from topic va'           | ['va']                      | 'value'       | 31            | false        | 'select value.prop from topic value'
    }
}
