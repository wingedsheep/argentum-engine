package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CreateChosenTokenEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateChosenTokenEffect.
 * Creates a creature token using the chosen color and creature type from the source permanent,
 * with dynamic power/toughness evaluated at resolution time.
 */
class CreateChosenTokenExecutor(
    private val dynamicAmountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<CreateChosenTokenEffect> {

    companion object {
        /** Scryfall token art mapped by creature type. */
        val TOKEN_IMAGES: Map<String, String> = mapOf(
            "Angel" to "https://cards.scryfall.io/large/front/c/7/c7f3264a-7b4a-4fef-af73-d4241742a4e8.jpg?1561758046",
            "Ape" to "https://cards.scryfall.io/large/front/8/3/8343e00c-5fc6-46a0-a238-3759338dced4.jpg?1562542388",
            "Assassin" to "https://cards.scryfall.io/large/front/8/9/89eb9f92-d189-4438-b6fe-cb253055d63e.jpg?1562539812",
            "Bear" to "https://cards.scryfall.io/large/front/0/a/0a21bc37-6f21-4dda-a313-a0d75696f7fc.jpg?1561756625",
            "Beast" to "https://cards.scryfall.io/large/front/c/e/ce45e037-5efb-4735-afee-12d7dc3127d1.jpg?1561758106",
            "Bird" to "https://cards.scryfall.io/large/front/2/6/26e0e196-36e6-4d7a-a76b-1c2a18270267.jpg?1561756807",
            "Cat" to "https://cards.scryfall.io/large/front/5/2/5252ab51-43e8-4b24-9830-de0ad9b9d3dc.jpg?1562164858",
            "Citizen" to "https://cards.scryfall.io/large/front/1/6/165164e7-5693-4d65-b789-8ed8a222365b.jpg?1547509191",
            "Cleric" to "https://cards.scryfall.io/large/front/9/a/9af74e26-69d7-4683-9a71-9b420c03d75f.jpg?1561757657",
            "Demon" to "https://cards.scryfall.io/large/front/0/8/084c3292-c3ec-431e-b162-309b5cf6309e.jpg?1561756614",
            "Djinn" to "https://cards.scryfall.io/large/front/f/2/f2e8077e-4400-4923-afe6-6ff5a51b5e91.jpg?1561758421",
            "Dragon" to "https://cards.scryfall.io/large/front/5/6/56c0fa29-fbcb-41fe-94ab-a39b1154c99b.jpg?1561757167",
            "Drake" to "https://cards.scryfall.io/large/front/6/5/65844e72-aa84-483f-b07e-9e34e798aaec.jpg?1625511529",
            "Dryad" to "https://cards.scryfall.io/large/front/7/4/74de70f2-93b6-4fc5-8c4d-464f880d3c54.jpg?1730838400",
            "Elemental" to "https://cards.scryfall.io/large/front/1/c/1c791044-be86-47d8-8c1b-299f6ab254e6.jpg?1562636696",
            "Elf" to "https://cards.scryfall.io/large/front/2/7/27b171ac-b2ef-4a80-92d1-6d9e71f3e3ca.jpg?1562636717",
            "Faerie" to "https://cards.scryfall.io/large/front/1/6/1666cae8-8750-4091-8e45-259e76268db9.jpg?1561756717",
            "Fish" to "https://cards.scryfall.io/large/front/1/f/1f3cea7c-d092-410d-8c32-af0f4c8bc878.jpg?1562541939",
            "Frog" to "https://cards.scryfall.io/large/front/e/3/e3c84944-23b8-40d7-9b25-c746b08b4dc4.jpg?1748704089",
            "Giant" to "https://cards.scryfall.io/large/front/0/6/0661166a-7d6c-4cba-8190-8d3eedf7f58c.jpg?1561756604",
            "Goat" to "https://cards.scryfall.io/large/front/9/0/90f67615-8a09-4ab9-9927-899a15e72c03.jpg?1561757576",
            "Goblin" to "https://cards.scryfall.io/large/front/f/b/fbd728ed-d5dc-44b3-9159-6c86cab3be0c.jpg?1761614931",
            "Griffin" to "https://cards.scryfall.io/large/front/9/6/96c36884-df5e-435d-9473-65da550535fb.jpg?1561757626",
            "Hippo" to "https://cards.scryfall.io/large/front/1/a/1aea5e0b-dc4e-4055-9e13-1dfbc25a2f00.jpg?1562844782",
            "Horror" to "https://cards.scryfall.io/large/front/5/a/5a4c5954-da2b-498a-9e86-eb4d6dc95cb8.jpg?1561757207",
            "Horse" to "https://cards.scryfall.io/large/front/b/c/bc944579-b6d8-40f7-8c46-146513960d61.jpg?1761614913",
            "Human" to "https://cards.scryfall.io/large/front/b/d/bd5cd362-34f9-445f-85e1-9f6694f0f90a.jpg?1561757948",
            "Illusion" to "https://cards.scryfall.io/large/front/5/d/5dcbf662-7263-414a-b64b-ccf9aab20faa.jpg?1562636807",
            "Imp" to "https://cards.scryfall.io/large/front/4/7/47a1385b-2be2-49a8-8400-186cd5525dad.jpg?1706217208",
            "Insect" to "https://cards.scryfall.io/large/front/a/a/aa47df37-f246-4f80-a944-008cdf347dad.jpg?1561757793",
            "Jellyfish" to "https://cards.scryfall.io/large/front/1/9/19ac0a35-fae7-49f9-ae96-4406df992dc9.jpg?1562639696",
            "Knight" to "https://cards.scryfall.io/large/front/b/f/bf9acfe1-de7a-48fe-aed3-28a72db6d1c0.jpg?1561757975",
            "Lizard" to "https://cards.scryfall.io/large/front/8/3/83790fd3-f371-494c-97a2-3aa469a399f1.jpg?1561757482",
            "Mercenary" to "https://cards.scryfall.io/large/front/5/f/5f04607f-eed2-462e-897f-82e41e5f7049.jpg?1712316319",
            "Merfolk" to "https://cards.scryfall.io/large/front/f/b/fb1b292b-2da6-4601-9f93-5eb273ce3a50.jpg?1562636943",
            "Minotaur" to "https://cards.scryfall.io/large/front/6/2/62a8926c-94d7-4399-ad38-f235bbfd1a7e.jpg?1561757290",
            "Monk" to "https://cards.scryfall.io/large/front/1/e/1e498e42-f55c-4afa-b2e2-02345f91cdb5.jpg?1561756357",
            "Octopus" to "https://cards.scryfall.io/large/front/1/9/19ac0a35-fae7-49f9-ae96-4406df992dc9.jpg?1562639696",
            "Pegasus" to "https://cards.scryfall.io/large/front/b/c/bc944579-b6d8-40f7-8c46-146513960d61.jpg?1761614913",
            "Pirate" to "https://cards.scryfall.io/large/front/4/6/46bf5e2b-869f-480e-ac37-1bdb40a92f8c.jpg?1665776397",
            "Rat" to "https://cards.scryfall.io/large/front/1/a/1a85fe9d-ef18-46c4-88b0-cf2e222e30e4.jpg?1562279130",
            "Rhino" to "https://cards.scryfall.io/large/front/1/3/1331008a-ae86-4640-b823-a73be766ac16.jpg",
            "Rogue" to "https://cards.scryfall.io/large/front/f/4/f44d5271-5d10-46b2-9ba2-5788d99de2e6.jpg?1562636888",
            "Serpent" to "https://cards.scryfall.io/large/front/5/4/54a1c6a9-3531-4432-9157-e4400dbc89fd.jpg",
            "Shapeshifter" to "https://cards.scryfall.io/large/front/1/a/1a7d89ca-8611-4bda-b5c8-0350ce091102.jpg",
            "Skeleton" to "https://cards.scryfall.io/large/front/6/8/6894192e-782b-49ef-b9fc-28b76e2268ab.jpg?1562636766",
            "Snake" to "https://cards.scryfall.io/large/front/8/3/83a6a142-f065-4a74-9a73-8105be29bc94.jpg?1562636831",
            "Soldier" to "https://cards.scryfall.io/large/front/b/1/b159b57d-bc52-4cef-ac7a-e364e40c3d03.jpg?1761614919",
            "Spider" to "https://cards.scryfall.io/large/front/7/d/7df0de51-8d05-475a-832e-de8a0f60849e.jpg?1562279134",
            "Spirit" to "https://cards.scryfall.io/large/front/1/4/14ef4815-3dfe-47b3-ad81-1506925280d3.jpg?1561756700",
            "Treefolk" to "https://cards.scryfall.io/large/front/2/a/2a3f0d52-34cd-4095-bfa0-bbf9562a8146.jpg",
            "Vampire" to "https://cards.scryfall.io/large/front/9/6/969eff58-d91e-49e2-a1e1-8f32b4598810.jpg?1562636856",
            "Wall" to "https://cards.scryfall.io/large/front/7/f/7f31debf-0b93-44c7-99b6-be441ba4e167.jpg?1562702163",
            "Warrior" to "https://cards.scryfall.io/large/front/1/2/12856d0c-240f-42c6-80dd-715ccf314645.jpg?1561756680",
            "Wizard" to "https://cards.scryfall.io/large/front/7/b/7b0b95ce-4821-4955-a27c-93471240f54b.jpg?1557575896",
            "Wraith" to "https://cards.scryfall.io/large/front/9/a/9afb58e8-f5a7-49f9-8287-77757bd3268c.jpg",
            "Wurm" to "https://cards.scryfall.io/large/front/9/f/9fc04b19-e636-4868-8224-a5da75ea01c8.jpg?1561757701",
            "Zombie" to "https://cards.scryfall.io/large/front/8/e/8e7b5995-ee8b-40d1-b157-8df96c02cc5b.jpg?1761614925",
        )
    }

    override val effectType: KClass<CreateChosenTokenEffect> = CreateChosenTokenEffect::class

    override fun execute(
        state: GameState,
        effect: CreateChosenTokenEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId ?: return ExecutionResult.success(state)
        val sourceEntity = state.getEntity(sourceId) ?: return ExecutionResult.success(state)

        // Read chosen color and creature type from source
        val chosenColor = sourceEntity.get<ChosenColorComponent>()?.color
        val chosenType = sourceEntity.get<ChosenCreatureTypeComponent>()?.creatureType

        val colors = if (chosenColor != null) setOf(chosenColor) else emptySet()
        val creatureTypes = if (chosenType != null) setOf(chosenType) else setOf("Creature")

        // Evaluate dynamic P/T
        val power = dynamicAmountEvaluator.evaluate(state, effect.dynamicPower, context)
        val toughness = dynamicAmountEvaluator.evaluate(state, effect.dynamicToughness, context)

        // Look up token art — try each creature type until a match is found
        val tokenImageUri = creatureTypes.firstNotNullOfOrNull { TOKEN_IMAGES[it] }

        val tokenId = EntityId.generate()
        val tokenName = "${creatureTypes.joinToString(" ")} Token"
        val tokenComponent = CardComponent(
            cardDefinitionId = "token:$tokenName",
            name = tokenName,
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.parse("Creature - ${creatureTypes.joinToString(" ")}"),
            baseStats = CreatureStats(power, toughness),
            colors = colors,
            ownerId = context.controllerId,
            imageUri = tokenImageUri
        )

        val container = ComponentContainer.of(
            tokenComponent,
            TokenComponent,
            ControllerComponent(context.controllerId),
            SummoningSicknessComponent
        )

        var newState = state.withEntity(tokenId, container)
        val battlefieldZone = ZoneKey(context.controllerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, tokenId)

        return ExecutionResult.success(newState)
    }
}
