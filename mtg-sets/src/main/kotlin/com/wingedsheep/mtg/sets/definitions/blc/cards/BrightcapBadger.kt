package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Brightcap Badger // Fungus Frolic
 * {3}{G}
 * Creature — Badger Druid
 * 3/4
 *
 * Each Fungus and Saproling you control has "{T}: Add {G}."
 * At the beginning of your end step, create a 1/1 green Saproling creature token.
 *
 * Adventure: Fungus Frolic — {2}{G}, Instant — Adventure
 * Create two 1/1 green Saproling creature tokens.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val BrightcapBadger = card("Brightcap Badger") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Badger Druid"
    oracleText = "Each Fungus and Saproling you control has \"{T}: Add {G}.\"\n" +
        "At the beginning of your end step, create a 1/1 green Saproling creature token."
    power = 3
    toughness = 4

    // Each Fungus and Saproling you control has "{T}: Add {G}."
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = Effects.AddMana(Color.GREEN),
                isManaAbility = true,
            ),
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl()
                    .withAnyOfSubtypes(listOf(Subtype.FUNGUS, Subtype.SAPROLING))
            )
        )
    }

    // At the beginning of your end step, create a 1/1 green Saproling creature token.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
            count = 1,
            imageUri = "https://cards.scryfall.io/normal/front/5/9/590abe94-9c71-4429-b1b5-8b5de877de03.jpg?1721427861"
        )
    }

    // Adventure: Fungus Frolic — Instant. Create two 1/1 green Saproling creature tokens.
    adventure("Fungus Frolic") {
        manaCost = "{2}{G}"
        typeLine = "Instant — Adventure"
        oracleText = "Create two 1/1 green Saproling creature tokens. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Saproling"),
                count = 2,
                imageUri = "https://cards.scryfall.io/normal/front/5/9/590abe94-9c71-4429-b1b5-8b5de877de03.jpg?1721427861"
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e843102-b8cc-4bc8-872e-79924197f4cc.jpg?1722207251"
        ruling("2024-07-26", "An adventurer card is a permanent card in every zone except the stack, as well as while on the stack if not cast as an Adventure. Ignore its alternative characteristics in those cases.")
        ruling("2024-07-26", "If a spell is cast as an Adventure, its controller exiles it instead of putting it into its owner's graveyard as it resolves. For as long as it remains exiled, that player may cast it as a permanent spell.")
        ruling("2024-07-26", "If an adventurer card ends up in exile for any other reason than by exiling itself while resolving, it won't give you permission to cast it as a permanent spell.")
    }
}
