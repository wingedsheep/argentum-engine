package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Strix Serenade
 * {U}
 * Instant
 * Counter target artifact, creature, or planeswalker spell. Its controller creates a 2/2 blue
 * Bird creature token with flying.
 *
 * The Bird is minted while the targeted spell is still on the stack (An Offer You Can't Refuse /
 * Undermine pattern): `TargetController` reads the stack object's `casterId`, which is where a
 * spell's controller lives — including after a control change on the spell. Once countered, the
 * card has no controller and the lookup would fall back to its owner. Both steps happen during
 * this one resolution, so no player can act between them, and the token is created whether or
 * not the spell can actually be countered (the 2024-06-07 ruling).
 */
val StrixSerenade = card("Strix Serenade") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target artifact, creature, or planeswalker spell. Its controller creates " +
        "a 2/2 blue Bird creature token with flying."

    spell {
        target(
            TargetFilter(
                GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Planeswalker,
                zone = Zone.STACK
            )
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            controller = EffectTarget.TargetController,
            imageUri = "https://cards.scryfall.io/normal/front/e/7/e7222593-5ade-4512-bfee-6126d4d0cc19.jpg?1783911117"
        ) then Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Filipe Pagliuso"
        flavorText = "Some birds sing to nature's beauty and the freedom of the skies. Others simply ask questions."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42ac5ac7-b2f9-4e6f-af41-7e42ac816374.jpg?1783911288"
        ruling(
            "2024-06-07",
            "Strix Serenade can target a spell that can't be countered. That spell won't be countered when " +
                "Strix Serenade resolves, but its controller will get a Bird token."
        )
    }
}
