package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.craft
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Master's Guide-Mural // Master's Manufactory (CR 702.167, The Lost Caverns of Ixalan)
 * {3}{W}{U}
 * Artifact // Artifact
 *
 * Front face — Master's Guide-Mural ({3}{W}{U}, Artifact)
 *   When this artifact enters, create a 4/4 white and blue Golem artifact creature token.
 *   Craft with artifact {4}{W}{W}{U} ({4}{W}{W}{U}, Exile this artifact, Exile another
 *   artifact you control or an artifact card from your graveyard: Return this card
 *   transformed under its owner's control. Craft only as a sorcery.)
 *
 * Back face — Master's Manufactory (Artifact)
 *   {T}: Create a 4/4 white and blue Golem artifact creature token. Activate only if this
 *   artifact or another artifact entered the battlefield under your control this turn.
 *
 * Implementation:
 *  - Front ETB: [Triggers.EntersBattlefield] + [Effects.CreateToken] (4/4 WU Golem,
 *    `artifactToken = true`, official LCI token art).
 *  - Craft: the `craft(...)` DSL helper — "Craft with artifact" is an exact-count craft
 *    (`minCount = 1, maxCount = 1`) over [GameObjectFilter.Artifact]; the material may come
 *    from the battlefield or the graveyard (CR 702.167a-b), and the helper's
 *    [com.wingedsheep.sdk.scripting.TimingRule.SorcerySpeed] enforces "Craft only as a sorcery".
 *  - Back {T} ability: same token effect, gated by [ActivationRestriction.OnlyIfCondition]
 *    on [Conditions.ArtifactEnteredBattlefieldThisTurn] — a pure per-turn ETB event tracker
 *    ("this artifact or another artifact" = any artifact under your control this turn,
 *    including the Manufactory itself entering via the craft return). As a noncreature
 *    artifact it has no summoning-sickness restriction on {T} abilities (CR 302.6 applies
 *    to creatures only), so it can be activated the turn it is crafted.
 */

/** The official LCI 4/4 white and blue Golem artifact creature token (TLCI #13). */
private val GolemToken = Effects.CreateToken(
    power = 4,
    toughness = 4,
    colors = setOf(Color.WHITE, Color.BLUE),
    creatureTypes = setOf("Golem"),
    artifactToken = true,
    imageUri = "https://cards.scryfall.io/normal/front/3/3/33a88096-dcfd-4acb-a092-b51507edd13e.jpg?1782731574"
)

private val MastersGuideMuralFront = card("Master's Guide-Mural") {
    manaCost = "{3}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, create a 4/4 white and blue Golem artifact creature token.\n" +
        "Craft with artifact {4}{W}{W}{U} ({4}{W}{W}{U}, Exile this artifact, Exile another artifact you control or an artifact card from your graveyard: Return this card transformed under its owner's control. Craft only as a sorcery.)"

    // ETB: create a 4/4 white and blue Golem artifact creature token.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = GolemToken
    }

    // "Craft with artifact" = exactly one artifact material.
    craft(
        filter = GameObjectFilter.Artifact,
        cost = "{4}{W}{W}{U}",
        materialDescription = "artifact",
        minCount = 1,
        maxCount = 1
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Racrufi"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7a41343-7cdb-49aa-a9d1-7460195355d8.jpg?1782694425"
    }
}

private val MastersManufactory = card("Master's Manufactory") {
    manaCost = ""
    colorIdentity = "WU"
    typeLine = "Artifact"
    oracleText = "{T}: Create a 4/4 white and blue Golem artifact creature token. Activate only if this artifact or another artifact entered the battlefield under your control this turn."

    activatedAbility {
        cost = Costs.Tap
        effect = GolemToken
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.ArtifactEnteredBattlefieldThisTurn)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Racrufi"
        flavorText = "\"The Fomori are a foul and distant memory, but their war machines taught us much about large-scale armorcraft.\"\n—Anim Pakal, Thousandth Moon"
        imageUri = "https://cards.scryfall.io/normal/back/f/7/f7a41343-7cdb-49aa-a9d1-7460195355d8.jpg?1782694425"
    }
}

val MastersGuideMural: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = MastersGuideMuralFront,
    backFace = MastersManufactory
)
