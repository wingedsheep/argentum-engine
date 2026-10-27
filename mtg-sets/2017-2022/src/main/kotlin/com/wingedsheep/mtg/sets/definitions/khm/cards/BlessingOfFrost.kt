package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Blessing of Frost
 * {3}{G}
 * Snow Sorcery
 * Distribute X +1/+1 counters among any number of creatures you control, where X is the amount of
 * {S} spent to cast this spell. Then draw a card for each creature you control with power 4 or
 * greater.
 *
 * Nothing is targeted: the distribution is chosen at resolution among creatures you control. X is
 * the snow mana spent on the cast (`SnowManaSpent`), 0 for a copy. The draw count is read after the
 * counters land, so creatures the counters pushed to power 4 or greater count.
 */
val BlessingOfFrost = card("Blessing of Frost") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Snow Sorcery"
    oracleText = "Distribute X +1/+1 counters among any number of creatures you control, where X is " +
        "the amount of {S} spent to cast this spell. Then draw a card for each creature you control " +
        "with power 4 or greater."

    spell {
        effect = Effects.DistributeCountersAmongFiltered(
            DynamicAmounts.snowManaSpent(),
            filter = GameObjectFilter.Creature.youControl()
        ) then Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.powerAtLeast(4)).count()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Mila Pesic"
        flavorText = "A friend of Jorn is never cold."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42f01f4d-491e-4f25-a017-a0d9c437ac8c.jpg?1783928219"
        ruling("2021-02-05", "Because Blessing of Frost doesn't target any creatures, you determine how the +1/+1 counters will be distributed as Blessing of Frost resolves.")
        ruling("2021-02-05", "Because none of the creatures are targeted, you can choose to put +1/+1 counters on a creature with protection from green, for example.")
        ruling("2021-02-05", "You determine how many creatures you control with power 4 or greater as Blessing of Frost is resolving, after +1/+1 counters have been put on creatures.")
        ruling("2021-02-05", "If you cast Blessing of Frost without spending snow mana, no +1/+1 counters will be distributed. You may still draw cards if your creatures are large enough.")
        ruling("2021-02-05", "If Blessing of Frost is copied, no mana was spent to cast the copy, so no +1/+1 counters will be distributed. You may still draw cards if your creatures are large enough.")
    }
}
