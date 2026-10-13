package xyz.bluspring.unitytranslate.mixin;

import java.io.PrintStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.github.ggerganov.whispercpp.WhisperCpp", remap = false)
public abstract class WhisperCppMixin {
    @Redirect(method = "fullTranscribe", at = @At(value = "INVOKE", target = "Ljava/io/PrintStream;println(Ljava/lang/String;)V"), require = 0, expect = 0)
    private void preventSegmentPrintlnSpam(PrintStream instance, String x) {
        // why this isn't behind a config, I have no idea.
    }
}
