package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.SpendAnyManaTypeForActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Drana and Linvala
 * {1}{W}{W}{B}
 * Legendary Creature — Vampire Angel
 * 3/4
 *
 * Flying, vigilance
 * Activated abilities of creatures your opponents control can't be activated.
 * Drana and Linvala has all activated abilities of all creatures your opponents control. You may
 * spend mana as though it were mana of any color to activate those abilities.
 *
 * The Sharkey, Tyrant of the Shire shape over creatures instead of lands, with no "except mana
 * abilities" carve-out on either side: opponents' creatures lose *all* their activated abilities
 * (mana abilities included), and Drana and Linvala gains all of them, mana abilities included.
 * The gained copies use Drana and Linvala as their source (CR 113.7 — "{T}: This deals 2 damage"
 * taps and is dealt by Drana and Linvala).
 *
 * "Spend mana as though it were mana of any color" is modelled with the any-type relaxation
 * (`substituteColor = null`), which turns colored pips into generic — exactly "any color" for
 * colored requirements. Known caveat: that relaxation also treats a `{C}` pip as generic, which
 * "any color" strictly doesn't (CR 118.14: "any type" adds "as though it were colorless"); the SDK has no
 * "any mana as any color" strength yet. Drana and Linvala has no activated abilities of its own,
 * so scoping the relaxation to the source covers exactly "those abilities".
 */
val DranaAndLinvala = card("Drana and Linvala") {
    manaCost = "{1}{W}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Vampire Angel"
    power = 3
    toughness = 4
    oracleText = "Flying, vigilance\n" +
        "Activated abilities of creatures your opponents control can't be activated.\n" +
        "Drana and Linvala has all activated abilities of all creatures your opponents control. " +
        "You may spend mana as though it were mana of any color to activate those abilities."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    staticAbility {
        ability = PreventActivatedAbilities(filter = GameObjectFilter.Creature.opponentControls())
    }
    staticAbility {
        ability = GainActivatedAbilitiesOfPermanents(
            grantedTo = GroupFilter.source(),
            sourceFilter = GameObjectFilter.Creature.opponentControls(),
            includeManaAbilities = true
        )
    }
    staticAbility {
        ability = SpendAnyManaTypeForActivatedAbilities(filter = GroupFilter.source())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Raluca Marinescu"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbda000f-3cf7-48b6-96ff-c23a3a64eab5.jpg?1783916955"
        ruling("2023-04-14", "Drana and Linvala gains only activated abilities. It doesn't gain triggered abilities or static abilities. Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" Some keywords are activated abilities; they have colons in their reminder texts.")
        ruling("2023-04-14", "If an activated ability references the creature that has it by name, treat Drana and Linvala's version of that ability as though it referenced Drana and Linvala by name instead. For example, if an opponent controls Onakke Javelineer, Drana and Linvala has the ability \"{T}: Drana and Linvala deals 2 damage to target player or battle.\"")
        ruling("2023-04-14", "Once an ability has been activated, it doesn't matter if Drana and Linvala loses that ability because the opposing creature with that ability leaves the battlefield (or, more optimistically, you gain control of it).")
    }
}
