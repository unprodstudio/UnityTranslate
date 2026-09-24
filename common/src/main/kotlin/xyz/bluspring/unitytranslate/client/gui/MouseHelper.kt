package xyz.bluspring.unitytranslate.client.gui

//? if < 26.3 {
/*import org.lwjgl.glfw.GLFW
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
*///? } else {
import org.lwjgl.sdl.SDLMouse
//? }

object MouseHelper {
    //? if >= 26.3 {
    private val arrowCursor = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT)
    private val pointerCursor = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_POINTER)
    private val horizontalResizeCursor = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_EW_RESIZE)
    private val verticalResizeCursor = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_NS_RESIZE)
    private val topLeftToBottomRightResizeCursor = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE)
    private val topRightToBottomLeftResizeCursor = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE)
    private val omniResizeCursor = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_MOVE)
    //? } else {
    /*private val arrowCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)
    private val pointerCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR)
    private val horizontalResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR)
    private val verticalResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR)
    private val topLeftToBottomRightResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR)
    private val topRightToBottomLeftResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR)
    private val omniResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_ALL_CURSOR)
    *///? }

    private var currentCursor = this.arrowCursor
    private var queuedCursor: Long? = null

    private fun setCursor(cursor: Long) {
        if (cursor == 0L)
            return

        this.queuedCursor = cursor
    }

    fun cursorToDefault() = this.setCursor(this.arrowCursor)
    fun cursorToPointer() = this.setCursor(this.pointerCursor)
    fun cursorToHorizontalResize() = this.setCursor(this.horizontalResizeCursor)
    fun cursorToVerticalResize() = this.setCursor(this.verticalResizeCursor)
    fun cursorToTopLeftToBottomRightResize() = this.setCursor(this.topLeftToBottomRightResizeCursor)
    fun cursorToTopRightToBottomLeftResize() = this.setCursor(this.topRightToBottomLeftResizeCursor)
    fun cursorToOmniResize() = this.setCursor(this.omniResizeCursor)

    fun tick() {
        var queued = this.queuedCursor
        if (queued == null && this.currentCursor != this.arrowCursor) {
            queued = this.arrowCursor
        }

        if (queued != null) {
            //? if >= 26.3 {
            SDLMouse.SDL_SetCursor(queued)
            //? } else {
            /*GLFW.glfwSetCursor(ClientPlatformProxy.instance.windowHandle, queued)
            *///? }
            this.currentCursor = queued
            this.queuedCursor = null
        }
    }

    fun close() {
        destroyCursor(this.arrowCursor)
        destroyCursor(this.pointerCursor)
        destroyCursor(this.horizontalResizeCursor)
        destroyCursor(this.verticalResizeCursor)
        destroyCursor(this.topLeftToBottomRightResizeCursor)
        destroyCursor(this.topRightToBottomLeftResizeCursor)
        destroyCursor(this.omniResizeCursor)
    }

    private fun destroyCursor(ptr: Long) {
        //? if >= 26.3 {
        SDLMouse.SDL_DestroyCursor(ptr)
        //? } else {
        /*GLFW.glfwDestroyCursor(ptr)
        *///? }
    }
}
