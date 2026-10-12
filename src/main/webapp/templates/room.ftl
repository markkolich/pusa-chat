<#import "layout.ftl" as layout>
<@layout.page title=name bodyClass="room" bodyId=id>
<div id="container">
    <div class="header"><p class="status">&nbsp;</p><input type="text" value="${name}" readonly></div>
    <div class="chat"><ul></ul></div>
</div>
<div id="footer">
    <div class="wrapper">
        <div class="inner">
            <p class="typing">&nbsp;</p>
            <form method="post" action="#" id="message-box">
                <input type="text" value=""> <input type="submit" value="Send">
            </form>
        </div>
    </div>
</div>
<div class="error registration" style="display:none;">
    <h4>Oops!</h4>
    <span>Sorry, we could not register your session with this chat room. The room may be full or unavailable. Please try again later.</span>
</div>
<div class="error unrecoverable" style="display:none;">
    <h4>Oops!</h4>
    <span>Sorry, something bad happened and this chat has ended. Please close this window and restart your chat session.</span>
</div>
<div class="error temporary" style="display:none;">
    <h4>Oops!</h4>
    <span>Sorry, we could not send your message. Please try again.</span>
</div>
</@layout.page>
