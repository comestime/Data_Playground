/**
 * Handles toggling the main navigation menu for small screens.
 */
jQuery( document ).ready( function( $ ) {
	var $masthead = $( '#masthead' ),
	    timeout = false;

	$.fn.smallMenu = function() {
		//$masthead.find( '#site-navigation2' ).removeClass( 'main-navigation' ).addClass( 'main-small-navigation' );
		$masthead.find( '.menu-toggle' ).removeClass('hide-mobile').addClass( 'show-mobile' );

		$( '.menu-toggle' ).unbind( 'click' ).click( function() {
			$masthead.find( '.nav-menu' ).toggle();
			$( this ).toggleClass( 'toggled-on' );
		} );
	};

	// Check viewport width on first load.
	if ( $( window ).width() < 520 )
		$.fn.smallMenu();

	// Check viewport width when user resizes the browser window.
	$( window ).resize( function() {
		var browserWidth = $( window ).width();

		if ( false !== timeout )
			clearTimeout( timeout );

		timeout = setTimeout( function() {
			if ( browserWidth < 601 ) {
				$.fn.smallMenu();
			} else {
				//$masthead.find( '#site-navigation2' ).removeClass( 'main-small-navigation' ).addClass( 'main-navigation' );
				$masthead.find( '.menu-toggle' ).removeClass('show-mobile').addClass( 'hide-mobile' );
				$masthead.find( '.nav-menu' ).removeAttr( 'style' );
			}
		}, 100 );
	} );
} );