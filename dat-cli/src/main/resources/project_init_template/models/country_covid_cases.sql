-- 这是一个 DuckDB 方言的示例查询SQL
select CAST(strptime(cases.date_rep, '%d/%m/%Y') AS DATE)    as report_date, -- 报告日期
       cases.cases                                           as cases, -- 病例数
       cases.deaths                                          as deaths, -- 死亡数
       country_codes.country                                 as country, -- 国家
       cases.geo_id                                          as geo_id -- 地理标识
from covid_cases as cases
         join country_codes
              on cases.geo_id = country_codes.alpha_2code
{% if start_date -%}
where CAST(strptime(cases.date_rep, '%d/%m/%Y') AS DATE) > '{{ start_date }}'
{% endif %}
;

/*
-- 这是一个 MySQL 方言的示例查询SQL
select CAST(STR_TO_DATE(cases.date_rep, '%d/%m/%Y') AS DATE) as report_date, -- 报告日期
       cases.cases                                           as cases, -- 病例数
       cases.deaths                                          as deaths, -- 死亡数
       country_codes.country                                 as country, -- 国家
       cases.geo_id                                          as geo_id -- 地理标识
from covid_cases as cases
         join country_codes
              on cases.geo_id = country_codes.alpha_2code
{% if start_date -%}
where CAST(STR_TO_DATE(cases.date_rep, '%d/%m/%Y') AS DATE) > '{{ start_date }}'
{% endif %}
;
*/