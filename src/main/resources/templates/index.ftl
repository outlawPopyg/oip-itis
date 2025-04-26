<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Векторный поиск</title>
</head>
<body style="text-align: center">
    <h1>Векторный поиск</h1>
    <br><hr>
    <form action="/search" method="get">
        <label>
            Найдется все!
            <input type="text" name="query">
        </label>
        <br><br>
        <button type="submit">Поиск</button>
        <br><br>
    </form>

    <#if pages??>
        <#list pages as page>
            <a href="${page}">${page}</a> <br><br>
        </#list>
    </#if>


</body>
</html>