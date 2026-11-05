package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dire Fleet Captain
 * {B}{R}
 * Creature — Orc Pirate
 * 2/2
 * Whenever this creature attacks, it gets +1/+1 until end of turn for each other attacking Pirate.
 *
 * The count spans every attacking Pirate on the battlefield (`Player.Each` — a Two-Headed Giant
 * teammate's attacking Pirates count, per the ruling), minus this creature itself, and is read once as
 * the trigger resolves.
 */
val DireFleetCaptain = card("Dire Fleet Captain") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Orc Pirate"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature attacks, it gets +1/+1 until end of turn for each other attacking Pirate."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val otherAttackingPirates: DynamicAmount = DynamicAmounts.battlefield(
            Player.Each,
            GameObjectFilter.Creature.withSubtype(Subtype.PIRATE).attacking(),
            excludeSelf = true,
        ).count()
        effect = Effects.ModifyStats(otherAttackingPirates, otherAttackingPirates, EffectTarget.Self)
        description = "Whenever this creature attacks, it gets +1/+1 until end of turn for each other attacking Pirate."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Yongjae Choi"
        flavorText = "Orcs are happiest under captains who steer toward battle. Orcs of the Dire Fleet are " +
            "downright jovial."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8200f8a3-d60b-470a-a558-0c3555d1f10b.jpg?1783935713"
        ruling(
            "2017-09-29",
            "Count the number of other attacking Pirates as the ability resolves to determine the size of the bonus.",
        )
        ruling(
            "2017-09-29",
            "Once the ability resolves, the bonus won't change later in the turn, even if the number of other " +
                "attacking Pirates does.",
        )
        ruling(
            "2017-09-29",
            "In a Two-Headed Giant game, attacking Pirates controlled by your teammate will count for Dire Fleet " +
                "Captain's ability.",
        )
    }
}
