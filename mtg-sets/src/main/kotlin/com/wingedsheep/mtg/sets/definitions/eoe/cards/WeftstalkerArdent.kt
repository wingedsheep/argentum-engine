package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Weftstalker Ardent
 * {2}{R}
 * Creature — Drix Artificer
 * Whenever another creature or artifact you control enters, this creature deals 1 damage to each opponent.
 * Warp {R} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)
 * 2/3
 */
val WeftstalkerArdent = card("Weftstalker Ardent") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Drix Artificer"
    oracleText = "Whenever another creature or artifact you control enters, this creature deals 1 damage to each opponent.\n" +
        "Warp {R} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"
    power = 2
    toughness = 3

    // Whenever another creature or artifact you control enters, this creature deals 1 damage to each opponent.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.CreatureOrArtifact.youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever another creature or artifact you control enters, this creature deals 1 damage to each opponent."
    }

    // Warp ability
    warp = "{R}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cddb48cc-8eb1-47ce-90f0-7aad1e93e2c4.jpg?1752947241"
    }
}
