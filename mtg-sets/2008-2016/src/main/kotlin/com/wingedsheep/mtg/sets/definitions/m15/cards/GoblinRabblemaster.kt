package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Rabblemaster — Magic 2015 #145
 * {2}{R}
 * Creature — Goblin Warrior
 * 2/2
 *
 * Other Goblin creatures you control attack each combat if able.
 * At the beginning of combat on your turn, create a 1/1 red Goblin creature token with haste.
 * Whenever this creature attacks, it gets +1/+0 until end of turn for each other attacking Goblin.
 *
 * "Other" in both places is the aggregate's own `excludeSelf`, not a subtract-one, so the counts stay
 * right if Rabblemaster stops being a Goblin. The attack count has no controller clause, so it is
 * [Player.Each] — a teammate's attacking Goblins count too. The bonus is locked in on resolution.
 */
val GoblinRabblemaster = card("Goblin Rabblemaster") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 2
    toughness = 2
    oracleText = "Other Goblin creatures you control attack each combat if able.\n" +
        "At the beginning of combat on your turn, create a 1/1 red Goblin creature token with haste.\n" +
        "Whenever this creature attacks, it gets +1/+0 until end of turn for each other attacking Goblin."

    staticAbility {
        ability = MustAttack(
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN).youControl(), excludeSelf = true)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            keywords = setOf(Keyword.HASTE),
            imageUri = "https://cards.scryfall.io/normal/front/9/8/98993a45-4aff-4f9b-a030-7d72fbb4ec6c.jpg?1783939141"
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(
            power = DynamicAmounts.battlefield(
                Player.Each,
                GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN).attacking(),
                excludeSelf = true
            ).count(),
            toughness = DynamicAmounts.fixed(0),
            target = EffectTarget.Self
        )
        description = "Whenever this creature attacks, it gets +1/+0 until end of turn for each other attacking Goblin."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee9c697e-d2c0-413b-9142-ecf5d7cf5322.jpg?1783939173"
        ruling(
            "2014-07-18",
            "Although Goblin Rabblemaster doesn't force itself to attack, if you control two of them, " +
                "they'll force each other to attack if able."
        )
        ruling(
            "2014-07-18",
            "The number of attacking Goblins is counted as the last ability resolves, and the bonus is locked in at that time."
        )
    }
}
