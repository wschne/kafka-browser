package com.rewe.digital.gui.controls.helper.autocomplete

import spock.lang.Specification
import spock.lang.Unroll

class SqlQueryAnalyzerSpec extends Specification {

    SqlQueryAnalyzer sqlQueryAnalyzer = new SqlQueryAnalyzer()

    @Unroll
    def "Get #expectedWords at position #position"() {
        expect:
        sqlQueryAnalyzer.getWordAtPosition(query, position) == expectedWords

        where:
        query                                    | position | expectedWords
        'select * from table where a'            | 13       | ['from']
        'select value from'                      | 13       | []
        'select * from table where a'            | 27       | ['a']
        'select * from table where a'            | 0        | []
        'select * from table where a'            | 1        | ['select']
        'select * from table where a'            | 100      | []
        'select * from table where vol.pay'      | 31       | ['vol', 'pay']
        'select * from table where vol.pay.date' | 31       | ['vol', 'pay', 'date']
        null                                     | 100      | []
    }

    @Unroll
    def "Is cursor at a dot: #expected at position #position"() {
        expect:
        sqlQueryAnalyzer.isLastTypedCharacterADot(query, position) == expected

        where:
        query                                                                                      | position | expected
        'select value. from address'                                                               | 12       | true
        'select value. from table where a'                                                         | 13       | true
        'select value. from table where a'                                                         | 14       | false
        'select value from table where a'                                                          | 12       | false
        'select key from address where val'                                                        | 33       | false
        'select key from address where value.payload.'                                             | 44       | true
        'select value.payload. from address where value.payload.additionalAddressInfo is not null' | 21       | true
    }
}
