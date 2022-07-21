package tools.empathy.libro.server.document

import kotlinx.css.Align
import kotlinx.css.BorderCollapse
import kotlinx.css.Color
import kotlinx.css.CssBuilder
import kotlinx.css.Display
import kotlinx.css.FlexDirection
import kotlinx.css.FontStyle
import kotlinx.css.JustifyContent
import kotlinx.css.LinearDimension
import kotlinx.css.ListStyleType
import kotlinx.css.Overflow
import kotlinx.css.Position
import kotlinx.css.QuotedString
import kotlinx.css.TextAlign
import kotlinx.css.VerticalAlign
import kotlinx.css.a
import kotlinx.css.alignItems
import kotlinx.css.background
import kotlinx.css.backgroundColor
import kotlinx.css.body
import kotlinx.css.border
import kotlinx.css.borderCollapse
import kotlinx.css.borderSpacing
import kotlinx.css.bottom
import kotlinx.css.button
import kotlinx.css.color
import kotlinx.css.content
import kotlinx.css.display
import kotlinx.css.em
import kotlinx.css.flexDirection
import kotlinx.css.fontFamily
import kotlinx.css.fontSize
import kotlinx.css.fontStyle
import kotlinx.css.height
import kotlinx.css.html
import kotlinx.css.justifyContent
import kotlinx.css.left
import kotlinx.css.lineHeight
import kotlinx.css.listStyleType
import kotlinx.css.margin
import kotlinx.css.marginBottom
import kotlinx.css.maxWidth
import kotlinx.css.minWidth
import kotlinx.css.opacity
import kotlinx.css.overflow
import kotlinx.css.overflowY
import kotlinx.css.p
import kotlinx.css.padding
import kotlinx.css.pct
import kotlinx.css.position
import kotlinx.css.properties.IterationCount
import kotlinx.css.properties.LineHeight
import kotlinx.css.properties.TextDecoration
import kotlinx.css.properties.Timing
import kotlinx.css.properties.animation
import kotlinx.css.properties.s
import kotlinx.css.properties.scaleY
import kotlinx.css.properties.transform
import kotlinx.css.properties.transition
import kotlinx.css.px
import kotlinx.css.rgb
import kotlinx.css.right
import kotlinx.css.table
import kotlinx.css.textAlign
import kotlinx.css.textDecoration
import kotlinx.css.top
import kotlinx.css.verticalAlign
import kotlinx.css.vh
import kotlinx.css.vw
import kotlinx.css.width

val preloaderCss = CssBuilder().apply {
    "html, body, div, span, applet, object, iframe, h1, h2, h3, h4, h5, h6, p, blockquote, pre, a, abbr, acronym, address, big, button, cite, code, del, dfn, em, img, ins, kbd, q, s, samp, small, strike, strong, sub, sup, tt, var, b, u, i, center, dl, dt, dd, ol, ul, li, fieldset, form, label, legend, table, caption, tbody, tfoot, thead, tr, th, td, article, aside, canvas, details, embed, figure, figcaption, footer, header, hgroup, menu, nav, output, ruby, section, summary, time, mark, audio, video" {
        margin(0.px)
        padding(0.px)
        border = "0"
        fontSize = 100.pct
        fontStyle = FontStyle.inherit
        verticalAlign = VerticalAlign.baseline
    }

    // HTML5 display-role reset for older browsers

    "article, aside, details, figcaption, figure, footer, header, hgroup, menu, nav, section" {
        display = Display.block
    }

    button {
        background = "none"
    }

    body {
        lineHeight = LineHeight.normal
    }

    "ol, ul" {
        listStyleType = ListStyleType.none
    }

    "blockquote, q" {
        put("quotes", "none")
    }

    "blockquote:before, blockquote:after" {
        content = QuotedString("")
        put("content", "none")
    }

    "q:before, q:after" {
        content = QuotedString("")
        put("content", "none")
    }

    table {
        borderCollapse = BorderCollapse.collapse
        borderSpacing = 0.px
    }

    // Preloader

    html {
        width = 100.pct
        height = 100.pct
        backgroundColor = Color.white
        put("touch-action", "manipulation")
    }

    a {
        // Prevents browser default color
        color = Color.inherit
        textDecoration = TextDecoration.none
    }

    p {
        marginBottom = 1.em
    }

    "::selection" {
        backgroundColor = Color.pink
        color = Color("#010101")
    }

    "h1, h2, h3, h4, h5, h6" {
        fontFamily = "'Helvetica Neue', Helvetica, Arial, sans-serif"
    }

    body {
        margin(0.px)
    }

    "body.Body--show-preloader .preloader" {
        opacity = 1
    }

    "body.Body--show-preloader #root" {
        opacity = 0
    }

    "#root" {
        put("-webkit-transition", "1s opacity")
        transition("opacity", 1.s)
        put("-webkit-transition-delay", ".5s")
        transition("delay", .5.s)
        opacity = 1
    }

    "#root.preloader-fixed" {
        position = Position.fixed
        top = LinearDimension("0")
        right = LinearDimension("0")
        bottom = LinearDimension("0")
        left = LinearDimension("0")
        height = 100.vh
        overflowY = Overflow.hidden
    }

    ".preloader" {
//        -webkit-box-align = center
//        -ms-flex-align = center
        alignItems = Align.center
        put("display", "-webkit-box")
        put("display", "-ms-flexbox")
        display = Display.flex
//        -webkit-box-orient = vertical
//        -webkit-box-direction = normal
//        -ms-flex-direction = column
        flexDirection = FlexDirection.column
        height = 100.vh
//        -webkit-box-pack = center
//        -ms-flex-pack = center
        justifyContent = JustifyContent.center
        opacity = 0
        overflow = Overflow.hidden
        position = Position.fixed
//        -webkit-transition = 1s opacity
        transition("opacity", 1.s)
        width = 100.pct
        top = LinearDimension("0")
        bottom = LinearDimension("0")
        left = LinearDimension("0")
        right = LinearDimension("0")
    }

    ".preloader__logo svg" {
        height = 100.pct
        maxWidth = 400.px
        minWidth = 150.px
        width = 20.vw
    }

    ".preloader__logo path" {
        put("fill", Color("#363835").toString())
    }

    ".preloader__logo__rect--motion" {
        put("fill", rgb(71, 86, 104).toString())
    }

    ".preloader__logo__rect--pro" {
        put("fill", rgb(84, 127, 75).toString())
    }

    ".preloader__logo__rect--con" {
        put("fill", rgb(134, 61, 61).toString())
    }

    ".spinner" {
        margin(0.px, LinearDimension.auto)
        width = 50.px
        height = 40.px
        textAlign = TextAlign.center
        fontSize = 10.px
    }

//    val skStretchDelay = KeyframesBuilder().apply {
//        from {
//            "0%, 40%, 100%" {
//                transform {
//                    scaleY(.4)
//                }
//            }
//        }
//        to {
//            "20%" {
//                transform {
//                    scaleY(1.0)
//                }
//            }
//        }
//    }

    "@keyframes sk-stretchdelay" {
        "0%, 40%, 100%" {
            transform {
                scaleY(.4)
            }
        }
        "20%" {
            transform {
                scaleY(1.0)
            }
        }
    }

    ".spinner > div" {
        backgroundColor = Color("#bbb")
        height = 100.pct
        width = 6.px
        display = Display.inlineBlock

        // -webkit-animation: sk-stretchdelay 1.2s infinite ease-in-out;
        // infinite
        animation("sk-stretchdelay", 1.2.s, Timing.easeInOut, iterationCount = IterationCount.infinite)
    }

    ".spinner .rect2" {
//        -webkit-animation-delay: -1.1s;
        put("animation-delay", "-1.1s")
    }

    ".spinner .rect3" {
//        -webkit-animation-delay: -1.0s;
        put("animation-delay", "-1.0s")
    }

    ".spinner .rect4" {
//        -webkit-animation-delay: -0.9s;
        put("animation-delay", "-0.9s")
    }

    ".spinner .rect5" {
//        -webkit-animation-delay: -0.8s;
        put("animation-delay", "-0.8s")
    }

//    "@-webkit-keyframes sk-stretchdelay" {
//        0%, 40%, 100% { -webkit-transform: scaleY(0.4) }
//        20% { -webkit-transform: scaleY(1.0) }
//    }

//    "@keyframes sk-stretchdelay" {
//        0%, 40%, 100% {
//        transform: scaleY(0.4);
//        -webkit-transform: scaleY(0.4);
//    }  20% {
//        transform: scaleY(1.0);
//        -webkit-transform: scaleY(1.0);
//    }
//    }
}.toString()
