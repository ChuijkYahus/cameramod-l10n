package net.mehvahdjukaar.vista.client.web;

import net.mehvahdjukaar.vista.client.textures.IWebTexture;
import net.minecraft.resources.ResourceLocation;

//one instance per url
//can create multiple texture views
public interface IMediaSession extends AutoCloseable {


    IWebTexture createTextureView(ResourceLocation resourceLocation);



}
