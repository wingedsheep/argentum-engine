package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Astor, Bearer of Blades
 * {2}{R}{W}
 * Legendary Creature — Human Warrior
 * 4/4
 * When Astor enters, look at the top seven cards of your library. You may reveal an Equipment or
 * Vehicle card from among them and put it into your hand. Put the rest on the bottom of your
 * library in a random order.
 * Equipment you control have equip {1}.
 * Vehicles you control have crew 1.
 *
 * "Have equip {1}" grants each Equipment a second equip ability ([GrantActivatedAbility]); "have
 * crew 1" grants each Vehicle a second crew ability, the projected `CREW_1` keyword
 * ([KeywordAbility.grantedCrew]). Neither replaces the printed one — the controller activates
 * whichever is cheaper.
 */
val AstorBearerOfBlades = card("Astor, Bearer of Blades") {
    manaCost = "{2}{R}{W}"
    typeLine = "Legendary Creature — Human Warrior"
    power = 4
    toughness = 4
    oracleText = "When Astor enters, look at the top seven cards of your library. You may reveal an " +
        "Equipment or Vehicle card from among them and put it into your hand. Put the rest on the " +
        "bottom of your library in a random order.\n" +
        "Equipment you control have equip {1}.\n" +
        "Vehicles you control have crew 1."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 7,
            filter = GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT) or
                GameObjectFilter.Any.withSubtype(Subtype.VEHICLE),
            prompt = "You may reveal an Equipment or Vehicle card and put it into your hand",
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility.equip(ManaCost.parse("{1}")),
            filter = GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
        )
    }

    staticAbility {
        ability = GrantKeyword(
            KeywordAbility.grantedCrew(1),
            GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.VEHICLE).youControl()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40f3a0ba-b917-488b-adbf-60a0d3c58a56.jpg?1783921288"
    }
}
