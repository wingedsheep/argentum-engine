package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unconventional Tactics — Hour of Devastation #27 (canonical printing)
 * {2}{W} · Sorcery
 *
 * Target creature gets +3/+3 and gains flying until end of turn.
 * Whenever a Zombie you control enters, you may pay {W}. If you do, return this card from your
 * graveyard to your hand.
 *
 * The second ability only functions from the graveyard (`triggerZone`), Lingering Phantom's shape.
 */
val UnconventionalTactics = card("Unconventional Tactics") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +3/+3 and gains flying until end of turn.\n" +
        "Whenever a Zombie you control enters, you may pay {W}. If you do, return this card from your graveyard to your hand."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 3, creature) then Effects.GrantKeyword(Keyword.FLYING, creature)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE).youControl()).enters()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.MayPay(
            cost = ManaCost.parse("{W}"),
            then = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52db5673-ecd3-4e58-8dc9-b42c76f80186.jpg?1783936055"
    }
}
