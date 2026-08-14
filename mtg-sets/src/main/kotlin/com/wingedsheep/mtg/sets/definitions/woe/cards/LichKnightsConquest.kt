package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lich-Knights' Conquest
 * {4}{B}
 * Sorcery
 *
 * Sacrifice any number of artifacts, enchantments, and/or tokens. Return that many creature cards
 * from your graveyard to the battlefield.
 *
 * The same sacrifice-then-spend shape as Hew the Entwood: [Effects.SacrificeAnyNumber] records what
 * was sacrificed on the resolving effect context, and the later step reads the count back through
 * [DynamicAmounts.permanentsSacrificedThisWay].
 *
 * Order matters and is load-bearing for two rulings:
 *  - The sacrifice happens *during resolution*, not as an additional cost — so a countered
 *    Conquest sacrifices nothing. Modelling it as the first step of the spell's own effect (rather
 *    than `additionalCost`) is what gets that right.
 *  - The graveyard is gathered *after* the sacrifice, so an artifact creature or enchantment
 *    creature sacrificed to the Conquest is already in the graveyard and may be one of the cards
 *    returned.
 *
 * `ChooseExactly(sacrificed)` is "return that many": mandatory, but capped at the collection size,
 * so sacrificing more permanents than you have creature cards simply returns everything. Zero
 * sacrifices gathers an empty selection and the spell does nothing, per "You can choose to
 * sacrifice zero permanents".
 *
 * [GameObjectFilter.ArtifactEnchantmentOrToken] is the same union bargain uses — a token of any
 * type qualifies, and an artifact *enchantment* counts once, not twice.
 */
val LichKnightsConquest = card("Lich-Knights' Conquest") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Sacrifice any number of artifacts, enchantments, and/or tokens. Return that many " +
        "creature cards from your graveyard to the battlefield."

    spell {
        effect = Effects.SacrificeAnyNumber(GameObjectFilter.ArtifactEnchantmentOrToken)
            .then(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.You,
                        filter = GameObjectFilter.Creature,
                    ),
                    storeAs = "graveyardCreatures",
                )
            )
            .then(
                SelectFromCollectionEffect(
                    from = "graveyardCreatures",
                    selection = SelectionMode.ChooseExactly(
                        DynamicAmounts.permanentsSacrificedThisWay()
                    ),
                    storeSelected = "returning",
                    prompt = "Return that many creature cards from your graveyard to the battlefield",
                )
            )
            .then(
                MoveCollectionEffect(
                    from = "returning",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                    underOwnersControl = true,
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Denis Zhbankov"
        flavorText = "In their rotting minds, they still quest for virtue and glory."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59cd67d3-3327-42ad-9db1-50e2f591818c.jpg?1783915106"

        ruling(
            "2023-09-01",
            "You sacrifice the artifacts, enchantments, and/or tokens as part of the resolution of " +
                "Lich-Knights' Conquest. It isn't an additional cost. If Lich-Knights' Conquest is " +
                "countered, you won't sacrifice anything."
        )
        ruling("2023-09-01", "You can choose to sacrifice zero permanents.")
        ruling(
            "2023-09-01",
            "If any abilities trigger as you sacrifice permanents, those abilities won't be put " +
                "onto the stack until after you've returned creature cards from your graveyard to " +
                "the battlefield."
        )
        ruling(
            "2023-09-01",
            "An artifact creature or enchantment creature sacrificed to Lich-Knights' Conquest may " +
                "be one of the cards chosen to be returned."
        )
    }
}
