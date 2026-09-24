package xyz.bluspring.unitytranslate.client.renderer

//? if >= 26.3 {
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.commands.RenderPass
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuSampler
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView

//? } else {
/*import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuSampler
import com.mojang.blaze3d.textures.GpuTextureView
*///? }

typealias GpuFormat = GpuFormat
typealias GpuBuffer = GpuBuffer
typealias RenderPipeline = RenderPipeline
typealias FilterMode = FilterMode
typealias GpuSampler = GpuSampler
typealias GpuTexture = GpuTexture
typealias GpuTextureView = GpuTextureView

//? if >= 26.3 {
val RenderTarget.useDepth: Boolean
    get() = this.hasDepth()

fun RenderPass.bindTexture(name: String, view: GpuTextureView, sampler: GpuSampler) {
    this.setUniform(name, view, sampler)
}

fun RenderPass.setPipeline(pipeline: RenderPipeline) {
    this.setPipeline(RenderSystem.getCompiledPipeline(pipeline))
}
//? }
