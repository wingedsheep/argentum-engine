package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mica, Reader of Ruins
 * {3}{R}
 * Legendary Creature — Human Artificer
 * 4/4
 *
 * Ward—Pay 3 life. (Whenever this creature becomes the target of a spell or ability an opponent
 *   controls, counter it unless that player pays 3 life.)
 * Whenever you cast an instant or sorcery spell, you may sacrifice an artifact. If you do, copy
 *   that spell and you may choose new targets for the copy.
 *
 * Modeling notes:
 * - "you may sacrifice an artifact. If you do, …" → [OptionalCostEffect]: the optional
 *   [SacrificeEffect] cost gates the copy. Declining (or having no artifact) skips the copy.
 * - The copy targets the triggering spell ([EffectTarget.TriggeringEntity]); [Effects.CopyTargetSpell]
 *   lets the controller choose new targets for the copy. The copy is created on the stack, so it's
 *   not "cast" and doesn't re-trigger Mica.
 */
val MicaReaderOfRuins = card("Mica, Reader of Ruins") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Artificer"
    power = 4
    toughness = 4
    oracleText = "Ward—Pay 3 life. (Whenever this creature becomes the target of a spell or " +
        "ability an opponent controls, counter it unless that player pays 3 life.)\n" +
        "Whenever you cast an instant or sorcery spell, you may sacrifice an artifact. If you do, " +
        "copy that spell and you may choose new targets for the copy."

    keywordAbility(KeywordAbility.wardLife(3))

    // Whenever you cast an instant or sorcery spell, you may sacrifice an artifact to copy it.
    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        effect = OptionalCostEffect(
            cost = SacrificeEffect(filter = GameObjectFilter.Artifact),
            ifPaid = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity),
        )
        description = "Whenever you cast an instant or sorcery spell, you may sacrifice an " +
            "artifact. If you do, copy that spell and you may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/949ad3ff-9e80-493c-a3ae-146b919bfcd7.jpg?1775937824"
    }
}
