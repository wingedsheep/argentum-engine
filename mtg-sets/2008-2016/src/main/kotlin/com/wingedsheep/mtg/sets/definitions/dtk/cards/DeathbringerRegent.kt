package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Deathbringer Regent — Dragons of Tarkir #96
 * {5}{B}{B} · Creature — Dragon · 5/6 · Rare
 *
 * Flying
 * When this creature enters, if you cast it from your hand and there are five or more other
 * creatures on the battlefield, destroy all other creatures.
 *
 * Both halves of the intervening-if (CR 603.4) are checked when the trigger would fire and again
 * on resolution: [Conditions.WasCastFromHand] (Reiver Demon's cast-origin marker) and a count of
 * creatures on the battlefield under any controller with the Regent itself excluded. The wipe is
 * [Effects.DestroyAll] over `Creature.notSourceItself()` (Myojin of Cleansing Fire's "all other
 * creatures") — it takes the five counted creatures with it, per the ruling.
 */
val DeathbringerRegent = card("Deathbringer Regent") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 6
    oracleText = "Flying\n" +
        "When this creature enters, if you cast it from your hand and there are five or more other " +
        "creatures on the battlefield, destroy all other creatures."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.All(
            Conditions.WasCastFromHand,
            Conditions.CompareAmounts(
                DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature, excludeSelf = true).count(),
                ComparisonOperator.GTE,
                5
            )
        )
        effect = Effects.DestroyAll(GameObjectFilter.Creature.notSourceItself())
        description = "When this creature enters, if you cast it from your hand and there are five " +
            "or more other creatures on the battlefield, destroy all other creatures."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75cad378-129f-41da-8db2-065fc45c76eb.jpg?1783938598"
        ruling(
            "2015-02-25",
            "If you cast a creature spell that enters the battlefield as a copy of Deathbringer " +
                "Regent, the enters-the-battlefield ability will trigger (assuming the \"five or more " +
                "other creatures\" requirement is also met)."
        )
        ruling(
            "2015-02-25",
            "Deathbringer Regent's last ability destroys all creatures except Deathbringer Regent, " +
                "including the five other creatures required for the ability to have an effect."
        )
    }
}
