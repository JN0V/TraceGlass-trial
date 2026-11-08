# TraceGlass — Overlay Transform Pipeline

### 4.1 OverlayTransformCalculator

**Purpose**: Convert detected marker positions into an overlay transformation matrix.

**State lifecycle**:
1. **Pre-reference** (0-3 markers first seen): Affine fallback (translate/rotate/scale from marker centroid)
2. **Reference established** (first 4-marker detection): Stores reference corners, computes paper aspect ratio
3. **Tracking** (subsequent frames): EMA smoothing + progressive degradation

**Progressive degradation**:

| Visible Markers | Strategy | Error Budget |
|----------------|----------|--------------|
| 4 | Full homography (DLT 4-point) | <0.1px |
| 3 | Constrained homography (Newton-Raphson) if f known, else affine delta | <5px at 20° tilt |
| 2 | Similarity transform (translation + rotation + scale) | <15px |
| 1 | Translation-only delta | <70px for 5° rotation |
| 0 | Hold last known position | 0px (frozen) |

**EMA smoothing**: `smooth[id] = lerp(smooth[id], detected[id], 0.12)` per-corner, applied before homography computation.

### 4.2 HomographySolver

**Pure Kotlin, no OpenCV dependency.**

| Method | Algorithm | Input | Output |
|--------|-----------|-------|--------|
| `solveHomography` | DLT (8×8 Gaussian elimination) | 4 src→dst point pairs | FloatArray(9) or null |
| `solveAffine` | 6×6 Gaussian elimination | 3 src→dst point pairs | FloatArray(9) or null |
| `estimateFocalLength` | Orthogonality + equal-norm constraints | paper+frame coords, cx, cy | Float? |
| `solveConstrainedHomography` | Newton-Raphson (2 quadratic constraints) | 3 points + f, cx, cy | FloatArray(9) or null |
| `correctAspectRatio` | Column norm ratio from H | paper+frame coords + f | Float? |

**Focal length auto-estimation**:
- Requires fronto-parallel reference (`isReferenceRectangular`: edge ratios within 8%)
- Uses H column orthogonality: `h₁ᵀ·ω·h₂ = 0` where `ω = K⁻ᵀK⁻¹`
- Equal-norm fallback: `|v₁|² = |v₂|²`
- Degenerate for near-square paper or horizontal-only tilt (H[6]≈0)

### 4.3 Coordinate Spaces

```
Frame Space (camera pixels, e.g. 1280×720)
    │  F2S matrix: pvScale + letterbox offset
    ▼
Screen Space (view pixels, e.g. 1080×1920)
    │  Homography: viewCorners → screenCorners
    ▼
View Space (centered overlay rect, paper AR)
    │  Manual transforms: rotate/scale before H
    ▼
Rendered Image
```

**Two rendering modes**:
- **Paper-corner mode** (`paperCornersFrame != null` + perspective enabled): `renderMatrix = mOffset × H × mView`
- **Affine mode**: `renderMatrix = translate × translate(center) × rotate × scale × translate(-center)`
