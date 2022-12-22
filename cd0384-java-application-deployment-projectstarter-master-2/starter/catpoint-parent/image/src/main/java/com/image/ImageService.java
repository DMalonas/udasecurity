package com.image;

import java.awt.image.BufferedImage;

/**
 * @author DMalonas
 */
public interface ImageService {
    boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}
