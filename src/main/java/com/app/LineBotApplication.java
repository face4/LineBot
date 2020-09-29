package com.app;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;

@Controller
@SpringBootApplication
@LineMessageHandler
public class LineBotApplication {
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Autowired
    private DataSource dataSource;

    private String datePattern = "MM/dd hh:mm:ss";

    public static void main(String[] args) {
        SpringApplication.run(LineBotApplication.class, args);
    }

    private TextMessage printAll(){
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
        try(Connection connection = dataSource.getConnection()){
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM health");
            int index = 0;
            while(resultSet.next()){
                sb.append(String.format("[%d] %s %3d:%3d:%3d\n",
                        index++,
                        sdf.format(resultSet.getDate(4)),
                        resultSet.getInt(1),
                        resultSet.getInt(2),
                        resultSet.getInt(3)));
            }
            String ret = sb.toString();
            ret = ret.substring(0, ret.length()-1);
            return new TextMessage(ret);
        }catch(Exception e){
            System.out.println("ERRORERROR.");
            return new TextMessage("DB error!\n" + e.getMessage());
        }
    }

    @EventMapping
    public TextMessage handleTextMessageEvent(MessageEvent<TextMessageContent> event) {
        System.out.println("event: " + event);
        String[] text = event.getMessage().getText().split("\n");
        if("out".equals(text[0])){
            return printAll();
        }
        try{
            int bsl = Integer.parseInt(text[0]);    // blood sugar level
            int inj = Integer.parseInt(text[1]);    // injection
            int car = Integer.parseInt(text[2]);    // carbohydrate
            try(Connection connection = dataSource.getConnection()){
                Statement statement = connection.createStatement();
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS health (bsl int, inj int, car int, date date)");
                statement.executeUpdate(String.format("INSERT INTO health VALUES (%d, %d, %d, now())", bsl, inj, car));
            }catch(Exception e){
                System.out.println("ERRORERROR.");
                return new TextMessage("DB error!\n" + dbUrl + "\n" + e.getMessage());
            }
        }catch(Exception e){
            return new TextMessage("invalid input!\n" + e.getMessage());
        }
        return new TextMessage("登録しました．");
    }

    @EventMapping
    public void handleDefaultMessageEvent(Event event) {
        System.out.println("event: " + event);
    }

    @Bean
    public DataSource dataSource() throws SQLException {
        if (dbUrl == null || dbUrl.isEmpty()) {
            return new HikariDataSource();
        } else {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            return new HikariDataSource(config);
        }
    }
}