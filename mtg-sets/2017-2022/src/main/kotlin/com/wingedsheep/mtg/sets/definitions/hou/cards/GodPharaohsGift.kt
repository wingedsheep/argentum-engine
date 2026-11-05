package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * God-Pharaoh's Gift — Hour of Devastation #161 (canonical printing)
 * {7} · Artifact
 *
 * At the beginning of combat on your turn, you may exile a creature card from your graveyard. If
 * you do, create a token that's a copy of that card, except it's a 4/4 black Zombie. It gains
 * haste until end of turn.
 *
 * Nothing targets: the creature card is chosen as the trigger resolves (ruling), as an optional
 * "up to one" selection over the creature cards in your graveyard. [moveTracked] keeps only a card
 * that actually reached exile, so choosing none (or an empty graveyard) creates nothing — "if you
 * do". The copy is Hour of Eternity's / The Scarab God's shape: "a 4/4 black Zombie" states colour
 * and creature type without "in addition to", so both replace the copied ones (CR 205.1a) via
 * `overrideColors` / `overrideSubtypes`; card types, supertypes and the mana cost are kept (ruling).
 * Haste is a separate until-end-of-turn grant on the created token (`CREATED_TOKENS`), not a
 * copiable keyword, so a later copy of the token does not inherit it.
 */
val GodPharaohsGift = card("God-Pharaoh's Gift") {
    manaCost = "{7}"
    typeLine = "Artifact"
    oracleText = "At the beginning of combat on your turn, you may exile a creature card from your " +
        "graveyard. If you do, create a token that's a copy of that card, except it's a 4/4 black " +
        "Zombie. It gains haste until end of turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.Pipeline {
            val graveyardCreatures = gather(
                CardSource.FromZone(zone = Zone.GRAVEYARD, filter = GameObjectFilter.Creature)
            )
            val chosen = chooseUpTo(
                1,
                from = graveyardCreatures,
                prompt = "You may exile a creature card from your graveyard to create a 4/4 black Zombie copy of it",
                selectedLabel = "Exile",
            )
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE))
            run(
                Effects.CreateTokenCopyOfTarget(
                    target = exiled.asTarget,
                    overridePower = 4,
                    overrideToughness = 4,
                    overrideColors = setOf(Color.BLACK),
                    overrideSubtypes = setOf(Subtype("Zombie")),
                )
            )
            run(Effects.GrantKeyword(Keyword.HASTE, EffectTarget.PipelineTarget(CREATED_TOKENS, 0)))
        }
        description = "At the beginning of combat on your turn, you may exile a creature card from your " +
            "graveyard. If you do, create a token that's a copy of that card, except it's a 4/4 black " +
            "Zombie. It gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2e85a9e-4c37-4721-b7ea-de3413ec39df.jpg?1783936002"
        ruling("2017-07-14", "The ability of God-Pharaoh's Gift doesn't target the creature card you'll exile. You choose one as the ability resolves. No player may take actions between the time you choose a creature card to exile and the time you create the token.")
        ruling("2017-07-14", "The token is a Zombie instead of its other types (unlike Zombies created by an eternalize ability) and is black instead of its other colors. Its power and toughness are 4/4. These are copiable values of the token that other effects may copy.")
        ruling("2017-07-14", "Unlike the tokens created by an eternalize ability, this token has the mana cost and thus mana value of the card it's copying.")
        ruling("2017-07-14", "If the card copied by the token had any \"when [this permanent] enters the battlefield\" abilities, then the token also has those abilities and will trigger them when it's created.")
    }
}
