package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vow to Erebor — The Hobbit #31
 * {1}{W} · Instant · Common
 *
 * Untap target creature you control. It gets +2/+2 until end of turn. If it's a Dwarf, you may
 * attach an Equipment you control to it.
 *
 * The Dwarf rider is a resolution-time check on the one target ([Conditions.TargetMatchesFilter]),
 * so a creature that only becomes a Dwarf in response still gets the attach, and a non-Dwarf target
 * skips it silently rather than fizzling the untap and the pump.
 *
 * The Equipment is **chosen, not targeted** — the oracle text says "an Equipment you control", not
 * "target Equipment". So it is gathered from the battlefield and picked with `chooseUpTo(1)`, whose
 * "up to" shape *is* the "you may": declining selects nothing and the attach is a no-op. The gather
 * passes `includeAttachments = true` because an Equipment already attached to another creature is
 * still one you control, and moving it is the main reason to cast this in combat.
 */
val VowToErebor = card("Vow to Erebor") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Untap target creature you control. It gets +2/+2 until end of turn. " +
        "If it's a Dwarf, you may attach an Equipment you control to it."

    spell {
        target = Targets.CreatureYouControl
        effect = Effects.Composite(
            Effects.Untap(EffectTarget.ContextTarget(0)),
            Effects.ModifyStats(2, 2, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(
                    GameObjectFilter.Creature.withSubtype(Subtype.DWARF),
                    targetIndex = 0
                ),
                effect = Effects.Pipeline {
                    val equipment = gather(
                        filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
                        player = Player.You,
                        includeAttachments = true
                    )
                    val chosen = chooseUpTo(
                        count = 1,
                        from = equipment,
                        useTargetingUI = true,
                        prompt = "You may attach an Equipment you control to the Dwarf",
                        selectedLabel = "Attach"
                    )
                    run(
                        Effects.AttachTargetEquipmentToCreature(
                            equipmentTarget = EffectTarget.PipelineTarget(chosen.key),
                            creatureTarget = EffectTarget.ContextTarget(0)
                        )
                    )
                }
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Andreia Ugrai"
        flavorText = "\"We shall soon, before the break of day, start on our long journey—a journey " +
            "from which some of us, or perhaps all of us, may never return.\"\n—Thorin"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d4f3eb5-fedf-45d6-8bd8-aacbe0ce33b2.jpg?1785497034"
    }
}
