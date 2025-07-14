Hobbie project for visual novel game
Plan
## 🏗️ **Architecture Overview:**

```
┌─────────────────┐
│   DrawContext   │  ← High-level drawing API
├─────────────────┤
│  RenderSystem   │  ← Game-specific rendering logic
├─────────────────┤
│ GlStateManager  │  ← Low-level OpenGL state tracking
├─────────────────┤
│   Raw OpenGL    │  ← Direct OpenGL calls
└─────────────────┘
```


