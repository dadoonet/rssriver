// (c) https://gist.github.com/823300, https://gist.github.com/379601

jQuery.placeholder = function() {
  $('[placeholder]').focus(function() {
    var input = $(this);
    if (input.hasClass('placeholder')) {
      input.val('');
      input.removeClass('placeholder');
    }
  }).blur(function() {
    var input = $(this);
    if (input.val() === '') {
      input.addClass('placeholder');
      input.val(input.attr('placeholder'));
    }
  }).blur().parents('form').submit(function() {
    $(this).find('[placeholder]').each(function() {
      var input = $(this);
      if (input.hasClass('placeholder')) {
        input.val('');
      }
    });
  });
  
  $(window).unload(function() {
    $('[placeholder]').val('');
  });
};
