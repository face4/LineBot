import com.app.LineBotApplication
import com.linecorp.bot.model.event.MessageEvent
import com.linecorp.bot.model.event.message.TextMessageContent
import spock.lang.Specification

import javax.sql.DataSource

class LineBotApplicationTest extends Specification{
    LineBotApplication service;

    def setup(){
        service = new LineBotApplication()
        service.dataSource = Mock(DataSource)
    }

    def "sample test"(){
        when:
            service.deleteAll()
        then:
            noExceptionThrown()
    }
}
