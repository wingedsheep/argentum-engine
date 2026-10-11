package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Echoing Assault
 * {4}{R}
 * Enchantment
 *
 * Creature tokens you control have menace.
 * Whenever you attack a player, choose target nontoken creature that's attacking that player.
 * Create a token that's a copy of that creature, except it's 1/1. The token enters tapped and
 * attacking that player. Sacrifice it at the beginning of the next end step.
 *
 * `Triggers.you.attacksAPlayer()` fires once per player attacked (the card's ruling) and binds
 * that player as `Player.TriggeringPlayer`, which both the target filter
 * (`attackingPlayer(...)` — that player, not their planeswalkers) and the copy's
 * `attackedPlayer` read. The copy carries the menace grant like any other creature token.
 */
val EchoingAssault = card("Echoing Assault") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Creature tokens you control have menace.\n" +
        "Whenever you attack a player, choose target nontoken creature that's attacking that player. " +
        "Create a token that's a copy of that creature, except it's 1/1. The token enters tapped and " +
        "attacking that player. Sacrifice it at the beginning of the next end step."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.MENACE,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Token).youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.you.attacksAPlayer()
        val thatPlayer = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.nontoken().attackingPlayer(thatPlayer))
        )
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            overridePower = 1,
            overrideToughness = 1,
            tapped = true,
            attacking = true,
            attackedPlayer = thatPlayer,
            sacrificeAtStep = Step.END
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3213bfee-c0a0-49a2-9080-cdaa09b414c5.jpg?1783910732"
    }
}
