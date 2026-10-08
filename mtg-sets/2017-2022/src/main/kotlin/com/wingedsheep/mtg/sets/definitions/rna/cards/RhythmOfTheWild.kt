package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rhythm of the Wild — Ravnica Allegiance #201 (canonical printing)
 * {1}{R}{G} · Enchantment
 *
 * Creature spells you control can't be countered.
 * Nontoken creatures you control have riot.
 *
 *  - The first line is Prowling Serpopard's [GrantCantBeCountered] over creature spells you
 *    control; a counterspell may still target them and its other effects still happen (ruling).
 *  - The second is a [GrantKeyword] of [Keyword.RIOT] over nontoken creatures you control. The
 *    engine's granted-riot synthesis asks counter-or-haste as each such creature enters — cast,
 *    or put onto the battlefield by an effect (a reanimation, a search) — reading the creature as
 *    it would exist on the battlefield (CR 614.12; Rusted Relic / Thassa rulings). Tokens don't
 *    get it.
 */
val RhythmOfTheWild = card("Rhythm of the Wild") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment"
    oracleText = "Creature spells you control can't be countered.\n" +
        "Nontoken creatures you control have riot. (They enter with your choice of a +1/+1 counter or haste.)"

    staticAbility {
        ability = GrantCantBeCountered(filter = GameObjectFilter.Creature.youControl())
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.RIOT,
            GroupFilter(GameObjectFilter.Creature.nontoken().youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Tomasz Jedruszek"
        flavorText = "Some view Domri's unlikely ascent as a sign of Ilharg the Raze-Boar's imminent return."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84062ce2-fea2-4e06-b83b-7cc597fb2a1b.jpg?1783933638"
        ruling("2019-05-03", "If Rhythm of the Wild leaves the battlefield at the same time that a nontoken creature enters the battlefield (most likely because that creature has a replacement effect, such as that of Rescuer Sphinx), that creature still gets a +1/+1 counter or haste.")
        ruling("2019-01-25", "A spell or ability that counters spells can still target a creature spell you control. When that spell or ability resolves, the creature spell won't be countered, but any additional effects of that spell or ability will still happen.")
        ruling("2019-01-25", "If you choose for the creature to gain haste, it gains haste indefinitely. It won't lose it as the turn ends or as another player gains control of it.")
        ruling("2019-01-25", "A noncreature card that happens to be entering the battlefield as a creature will have riot (for example, Rusted Relic while you control three other artifacts). Similarly, a creature card entering the battlefield as a noncreature permanent won't have riot (for example, Thassa, God of the Sea while your other permanents contribute only four to your devotion to blue).")
        ruling("2019-01-25", "If a creature enters the battlefield with two instances of riot, you may choose to have it get two +1/+1 counters, one +1/+1 counter and haste, or two instances of haste. Multiple instances of haste on the same creature are redundant, but we're not going to tell the Gruul how to live their lives.")
        ruling("2019-01-25", "Riot is a replacement effect. Players can't respond to your choice of +1/+1 counter or haste, and they can't take actions while the creature is on the battlefield without one or the other.")
        ruling("2019-01-25", "If a nontoken, noncreature permanent becomes a creature after it's already on the battlefield, it will have riot but it will be too late for the replacement effect to have any effect.")
        ruling("2019-01-25", "If a creature entering the battlefield has riot but can't have a +1/+1 counter put onto it, it gains haste.")
        ruling("2019-01-25", "Once a creature with riot has entered the battlefield, it keeps its +1/+1 counter or haste even if it loses riot.")
    }
}
