package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Damage Control Crew
 * {3}{G}
 * Creature — Human Citizen
 * 3/3
 * When this creature enters, choose one —
 * • Repair — Return target card with mana value 4 or greater from your graveyard to your hand.
 * • Impound — Exile target artifact or enchantment.
 *
 * A modal "choose one" ETB built with [ModalEffect.chooseOne] (same shape as the just-added
 * Daily Bugle Reporters):
 *  - Repair targets a single card of any type in your graveyard restricted to
 *    `manaValueAtLeast(4)` and [Effects.Move]s it to your hand.
 *  - Impound targets a battlefield artifact-or-enchantment ([TargetPermanent] evaluates the
 *    Artifact/Enchantment predicate against projected state) and [Effects.Exile]s it.
 */
val DamageControlCrew = card("Damage Control Crew") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Citizen"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, choose one —\n" +
        "• Repair — Return target card with mana value 4 or greater from your graveyard to your hand.\n" +
        "• Impound — Exile target artifact or enchantment."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                effect = Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND),
                target = TargetObject(
                    filter = TargetFilter(
                        GameObjectFilter.Any.ownedByYou().manaValueAtLeast(4),
                        zone = Zone.GRAVEYARD
                    )
                ),
                description = "Repair — Return target card with mana value 4 or greater from your graveyard to your hand."
            ),
            Mode.withTarget(
                effect = Effects.Exile(EffectTarget.ContextTarget(0)),
                target = TargetPermanent(
                    filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment)
                ),
                description = "Impound — Exile target artifact or enchantment."
            )
        )
        description = "When this creature enters, choose one — Repair — Return target card with mana value 4 or greater from your graveyard to your hand. • Impound — Exile target artifact or enchantment."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Borja Pindado"
        flavorText = "\"We got two hours until Spidey's webbing dissolves. I need engineers on-site now.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad2cab87-691d-44fe-ab2f-33760b1feb0f.jpg?1783905330"
    }
}
