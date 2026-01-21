import { useRef, useState } from 'react'
import { Mesh } from 'three'
import { useFrame } from '@react-three/fiber'
import { Text } from '@react-three/drei'
import type { ClientCard } from '../../types'
import { useCardTexture, useCardBackTexture, getCardFrameColor } from '../../hooks/useCardTexture'
import { useGameStore } from '../../store/gameStore'
import { useHasLegalActions } from '../../store/selectors'
import { CARD_WIDTH, CARD_HEIGHT, CARD_DEPTH } from '../zones/ZoneLayout'
import { CardHighlight } from './CardHighlight'
import { PowerToughnessDisplay } from './PowerToughnessDisplay'
import { CounterDisplay } from './CounterDisplay'

export type CardHighlightType = 'legal' | 'selected' | 'target' | 'active' | undefined

interface Card3DProps {
  card: ClientCard
  position: [number, number, number]
  rotation?: [number, number, number]
  scale?: number
  faceDown?: boolean
  interactive?: boolean
  hoverLift?: boolean
  highlight?: CardHighlightType
}

/**
 * 3D card representation.
 *
 * Renders a card with:
 * - Front face with texture or placeholder
 * - Back face with card back texture
 * - Tapped rotation
 * - Hover effects
 * - Selection/target highlighting
 * - Power/toughness overlay
 * - Counter indicators
 */
export function Card3D({
  card,
  position,
  rotation = [0, 0, 0],
  scale = 1,
  faceDown = false,
  interactive = true,
  hoverLift = false,
  highlight,
}: Card3DProps) {
  const meshRef = useRef<Mesh>(null)
  const [isHovered, setIsHovered] = useState(false)

  // Zustand actions
  const selectCard = useGameStore((state) => state.selectCard)
  const hoverCard = useGameStore((state) => state.hoverCard)
  const selectedCardId = useGameStore((state) => state.selectedCardId)

  // Check if this card has legal actions
  const hasLegalActions = useHasLegalActions(card.id)

  // Load textures
  const frontTexture = useCardTexture(faceDown ? null : card.name)
  const backTexture = useCardBackTexture()

  // Determine if card is selected
  const isSelected = selectedCardId === card.id

  // Calculate actual highlight
  const actualHighlight = highlight ?? (isSelected ? 'selected' : hasLegalActions ? 'legal' : undefined)

  // Hover animation
  useFrame(() => {
    if (!meshRef.current || !hoverLift) return

    const targetY = isHovered ? 0.15 : 0
    meshRef.current.position.y += (targetY - meshRef.current.position.y) * 0.1
  })

  // Calculate tapped rotation (around Y axis for top-down view)
  const tappedRotation = card.isTapped ? Math.PI / 2 : 0

  // Frame color for fallback
  const frameColor = getCardFrameColor(card.colors)

  // Card dimensions
  const width = CARD_WIDTH * scale
  const height = CARD_HEIGHT * scale
  const depth = CARD_DEPTH * scale

  const handleClick = (e: { stopPropagation: () => void }) => {
    if (!interactive) return
    e.stopPropagation()
    selectCard(isSelected ? null : card.id)
  }

  const handlePointerOver = (e: { stopPropagation: () => void }) => {
    if (!interactive) return
    e.stopPropagation()
    setIsHovered(true)
    hoverCard(card.id)
    document.body.style.cursor = 'pointer'
  }

  const handlePointerOut = () => {
    if (!interactive) return
    setIsHovered(false)
    hoverCard(null)
    document.body.style.cursor = 'auto'
  }

  // For top-down view, card lies flat on the table with face up
  // Apply tapped rotation around Y axis
  const groupRotation: [number, number, number] = [0, rotation[1] + tappedRotation, 0]

  return (
    <group position={position} rotation={groupRotation}>
      {/* Highlight glow */}
      {actualHighlight && (
        <CardHighlight
          type={actualHighlight}
          width={width}
          height={height}
          isHovered={isHovered}
        />
      )}

      {/* Main card mesh - rotated to lie flat */}
      <mesh
        ref={meshRef}
        onClick={handleClick}
        onPointerOver={handlePointerOver}
        onPointerOut={handlePointerOut}
        rotation={[-Math.PI / 2, 0, 0]}
        castShadow
        receiveShadow
      >
        <boxGeometry args={[width, height, depth]} />

        {/* Materials for each face: +X, -X, +Y (front/face), -Y (back), +Z, -Z */}
        <meshStandardMaterial attach="material-0" color={frameColor} /> {/* Right */}
        <meshStandardMaterial attach="material-1" color={frameColor} /> {/* Left */}

        {/* Top face (Y+) - card front when lying flat */}
        {frontTexture && !faceDown ? (
          <meshStandardMaterial
            attach="material-2"
            map={frontTexture}
            roughness={0.4}
            metalness={0.1}
          />
        ) : (
          <meshStandardMaterial
            attach="material-2"
            color={faceDown ? '#2a2a4e' : frameColor}
            roughness={0.6}
          />
        )}

        {/* Bottom face (Y-) - card back when lying flat */}
        {backTexture ? (
          <meshStandardMaterial
            attach="material-3"
            map={backTexture}
            roughness={0.4}
            metalness={0.1}
          />
        ) : (
          <meshStandardMaterial
            attach="material-3"
            color="#1a1a2e"
            roughness={0.6}
          />
        )}

        <meshStandardMaterial attach="material-4" color={frameColor} /> {/* Front Z+ */}
        <meshStandardMaterial attach="material-5" color={frameColor} /> {/* Back Z- */}
      </mesh>

      {/* Card name (for placeholder without texture) */}
      {!frontTexture && !faceDown && (
        <Text
          position={[0, depth / 2 + 0.02, -height * 0.35]}
          rotation={[-Math.PI / 2, 0, 0]}
          fontSize={0.18 * scale}
          color="#000000"
          anchorX="center"
          anchorY="middle"
          maxWidth={width * 0.9}
        >
          {card.name}
        </Text>
      )}

      {/* Power/Toughness display */}
      {!faceDown && card.power !== null && card.toughness !== null && (
        <PowerToughnessDisplay
          power={card.power}
          toughness={card.toughness}
          damage={card.damage}
          position={[width * 0.35, depth / 2 + 0.01, height * 0.35]}
          scale={scale}
        />
      )}

      {/* Counter display */}
      {!faceDown && Object.keys(card.counters).length > 0 && (
        <CounterDisplay
          counters={card.counters}
          position={[-width * 0.35, depth / 2 + 0.01, -height * 0.2]}
          scale={scale}
        />
      )}

      {/* Summoning sickness indicator */}
      {!faceDown && card.hasSummoningSickness && card.cardTypes.includes('CREATURE') && (
        <mesh position={[0, depth / 2 + 0.015, 0]} rotation={[-Math.PI / 2, 0, 0]}>
          <ringGeometry args={[width * 0.3, width * 0.32, 32]} />
          <meshBasicMaterial color="#888888" transparent opacity={0.5} />
        </mesh>
      )}
    </group>
  )
}
