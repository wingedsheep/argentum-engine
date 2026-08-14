package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Thranduil, Sindarin Liege // Silvan Rally — The Hobbit #166
 * {2}{G/U}{G/U} · Legendary Creature — Elf Noble · Uncommon
 * 2/3
 *
 * Other Elves you control get +1/+1.
 * Landfall — Whenever a land you control enters, create a 1/1 green Elf creature token.
 *
 * Adventure: Silvan Rally — {1}{G/U}{G/U}, Sorcery — Adventure
 * Mill four cards, then put up to two land cards from among them into your hand.
 *
 * Modeling notes:
 *  - The lord is `excludeSelf = true` on the [GroupFilter]: Thranduil is himself an Elf and the
 *    printed wording is "**Other** Elves", so he must not pump himself. Same shape as Mabel, Heir to
 *    Cragflame; see [ThranduilsCompany] for the sibling landfall wiring in this set.
 *  - **Landfall** is [Triggers.LandYouControlEnters] — every land entering under your control, not
 *    just the ones you play, so a land put onto the battlefield by another spell counts.
 *  - The Adventure is a mill → select → move pipeline over the *same* collection: the milled cards
 *    are already in the graveyard when the selection happens ("from among them" is a reference to
 *    those four specific cards, not to the graveyard at large), which is why the second step reads
 *    `from = "milled"` rather than re-gathering. `ChooseUpTo(2)` carries the "up to", so milling
 *    zero or one land — or choosing to take none — is legal.
 *  - `Patterns.Library.mill` is used rather than a hand-rolled gather so the move is flagged as a
 *    real mill (`isMill = true`) and any "whenever you mill" watcher sees it.
 *  - (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 *    caster cast it as the creature while it remains in exile.)
 */
val ThranduilSindarinLiege = card("Thranduil, Sindarin Liege") {
    manaCost = "{2}{G/U}{G/U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elf Noble"
    power = 2
    toughness = 3
    oracleText = "Other Elves you control get +1/+1.\n" +
        "Landfall — Whenever a land you control enters, create a 1/1 green Elf creature token."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.ELF).youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elf"),
            imageUri = "https://cards.scryfall.io/normal/front/7/6/761c7c31-c6c5-44e2-a845-f590542b6eda.jpg?1785497812",
        )
        description = "Landfall — Whenever a land you control enters, create a 1/1 green Elf " +
            "creature token."
    }

    adventure("Silvan Rally") {
        manaCost = "{1}{G/U}{G/U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Mill four cards, then put up to two land cards from among them into your " +
            "hand. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.Composite(
                Patterns.Library.mill(4),
                SelectFromCollectionEffect(
                    from = "milled",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                    filter = GameObjectFilter.Land,
                    storeSelected = "toHand",
                    showAllCards = true,
                    prompt = "Put up to two land cards into your hand",
                    selectedLabel = "Put in hand",
                    remainderLabel = "Leave in graveyard"
                ),
                MoveCollectionEffect(
                    from = "toHand",
                    destination = CardDestination.ToZone(Zone.HAND)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/481870ee-d1f7-421b-86e1-570ea933bbbc.jpg?1785412747"
    }
}
