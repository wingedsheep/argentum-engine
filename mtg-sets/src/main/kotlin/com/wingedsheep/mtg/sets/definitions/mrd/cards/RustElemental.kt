package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rust Elemental — Mirrodin #234
 * {4} · Artifact Creature — Elemental · 4/4
 *
 * Flying
 * At the beginning of your upkeep, sacrifice another artifact. If you can't, tap this creature
 * and you lose 4 life.
 *
 * A four-mana 4/4 flier that eats your board. The "if you can't" clause is the whole card, so it
 * is modelled as an explicit [ConditionalEffect] rather than leaning on the sacrifice silently
 * fizzling: the condition asks whether you control an artifact *other than* Rust Elemental, and
 * only the failing branch taps it and drains 4.
 *
 * Both `excludeSelf`/`excludeSource` flags are load-bearing — Rust Elemental is itself an artifact,
 * so without them it would satisfy its own requirement and happily sacrifice itself every upkeep.
 *
 * The sacrifice is mandatory when it's possible (no "may"), and the penalty branch taps rather than
 * requiring the creature to be untapped: if Rust Elemental is already tapped, the tap does nothing
 * and you still lose 4 life.
 */
val RustElemental = card("Rust Elemental") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "At the beginning of your upkeep, sacrifice another artifact. If you can't, tap this " +
        "creature and you lose 4 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = ConditionalEffect(
            condition = Conditions.YouControl(GameObjectFilter.Artifact, excludeSelf = true),
            effect = SacrificeEffect(GameObjectFilter.Artifact, excludeSource = true),
            elseEffect = Effects.Composite(
                Effects.Tap(EffectTarget.Self),
                Effects.LoseLife(4, EffectTarget.PlayerRef(Player.You))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/967a08cd-72b3-4b3f-9f23-15697704acb3.jpg?1783944506"
    }
}
