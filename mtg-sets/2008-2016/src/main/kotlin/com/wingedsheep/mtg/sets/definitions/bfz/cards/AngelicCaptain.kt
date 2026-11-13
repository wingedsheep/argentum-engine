package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angelic Captain
 * {3}{R}{W}
 * Creature — Angel Ally
 * 4/3
 * Flying
 * Whenever this creature attacks, it gets +1/+1 until end of turn for each other attacking Ally.
 *
 * The count has no controller clause, so it is [Player.Each] — a teammate's attacking Allies count
 * too (per the 2015-08-25 ruling). "Other" is the aggregate's own `excludeSelf`, and the count is
 * taken once as the ability resolves, so later changes to the attacking Allies don't move the bonus.
 */
val AngelicCaptain = card("Angelic Captain") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "WR"
    typeLine = "Creature — Angel Ally"
    power = 4
    toughness = 3
    oracleText = "Flying\nWhenever this creature attacks, it gets +1/+1 until end of turn for each other attacking Ally."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val otherAttackingAllies = DynamicAmounts.battlefield(
            Player.Each,
            GameObjectFilter.Creature.withSubtype("Ally").attacking(),
            excludeSelf = true
        ).count()
        effect = Effects.ModifyStats(
            power = otherAttackingAllies,
            toughness = otherAttackingAllies,
            target = EffectTarget.Self
        )
        description = "Whenever this creature attacks, it gets +1/+1 until end of turn for each other attacking Ally."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Volkan Baǵa"
        flavorText = "As the soldiers beneath her raised their voices, the angel spread her wings, their span seeming to cover the sky."
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef0baf56-816a-4e0a-beb3-73b51487ff00.jpg?1783938181"
        ruling("2015-08-25", "Count the number of other attacking Allies as the ability resolves to determine the size of the bonus. Attacking Allies controlled by a teammate will count toward this total.")
        ruling("2015-08-25", "Once the ability resolves, the bonus won't change later in the turn, even if the number of other attacking Allies does.")
    }
}
