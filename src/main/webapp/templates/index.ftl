<#import "layout.ftl" as layout>
<#--
  HtmlMinifier removes line breaks rather than turning them into spaces, so inline elements
  that need a space between them (label, text box, button) share a line with explicit spaces.
-->
<@layout.page title="Pusa Chat" bodyClass="homepage">
<h2>Pusa Chat</h2>
<div class="panel join" style="display:block;">
    <h3>Join Room</h3>
    <form method="get" action="#" class="join" autocomplete="off">
        <label for="join-key">Room Key:</label> <input type="text" value="${key!}" name="key" id="join-key" maxlength="36"> <input type="submit" value="Join Room">
    </form>
    <p class="direct"><a href="javascript:void(0)" class="create">Create new room</a></p>
</div>
<div class="panel create" style="display:none;">
    <h3>Create New Room</h3>
    <form method="get" action="#" class="create" autocomplete="off">
        <label for="create-key">Room Key:</label> <input type="text" value="" name="key" id="create-key" maxlength="36">
        <p class="url" style="display:none;">&nbsp;</p>
        <p class="buttons"><input type="submit" value="Create Room">&nbsp;<input type="button" class="random" value="Generate Random"></p>
    </form>
    <p class="direct"><a href="javascript:void(0)" class="join">Join room</a></p>
</div>
<h4>&copy; Copyright ${.now?string("yyyy")} Mark S. Kolich</h4>
</@layout.page>
