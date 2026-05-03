import Lightbox from 'yet-another-react-lightbox'
import Zoom from 'yet-another-react-lightbox/plugins/zoom'
import 'yet-another-react-lightbox/styles.css'
import type { Slide } from 'yet-another-react-lightbox'

interface ImageLightboxProps {
  open: boolean
  close: () => void
  index: number
  slides: Slide[]
}

export default function ImageLightbox({ open, close, index, slides }: ImageLightboxProps) {
  return (
    <Lightbox
      open={open}
      close={close}
      index={index}
      slides={slides}
      plugins={[Zoom]}
      carousel={{ finite: slides.length <= 1 }}
      controller={{ closeOnBackdropClick: true }}
    />
  )
}
