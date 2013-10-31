ImageManager
===============
### Reference Library
 * LightBoxPhotoProcessing: https://github.com/waveface/PhotoProcessing
 
### Sample Code

```
ImageManager mImageManager = ImageManager.getInstance(context);

ImageAttribute attr = new ImageAttribute(mMockup);
attr.setDoneScaleType(ImageView.ScaleType.FIT_CENTER);
attr.setLoadFromThread(true);
attr.setApplyWithAnimation(true);
attr.setDefaultResId(R.drawable.ic_project_default);
attr.setFailResId(R.drawable.ic_project_default);

mImageManager.getImage(url, attr);
```
