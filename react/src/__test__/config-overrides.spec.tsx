import overrides from '../../config-overrides';

describe('config-overrides', () => {
    describe('devServer', () => {
        const OLD_ENV = process.env;

        beforeEach(() => {
            jest.resetModules();
            process.env = { ...OLD_ENV };
        });

        afterAll(() => {
            process.env = OLD_ENV;
        });

        it('injects forwarded headers when INJECT_TEST_HEADERS=true', () => {
            process.env.INJECT_TEST_HEADERS = 'true';

            const configFunction = () => ({ proxy: { '/other': { target: 'http://x' } } });
            const makeConfig = overrides.devServer(configFunction);
            const result = makeConfig({}, 'localhost');

            expect(result.proxy['/api']).toBeDefined();
            expect(result.proxy['/api'].target).toBe('http://localhost:8081');
            expect(result.proxy['/api'].headers['X-Forwarded-Email']).toBe('test@example.com');
            expect(result.proxy['/api'].headers['X-Forwarded-Groups']).toContain('role:manage-users');
        });

        it('does not inject headers when INJECT_TEST_HEADERS is not true', () => {
            process.env.INJECT_TEST_HEADERS = 'false';

            const configFunction = () => ({ proxy: {} });
            const makeConfig = overrides.devServer(configFunction);
            const result = makeConfig({}, 'localhost');

            expect(result.proxy['/api']).toBeDefined();
            expect(result.proxy['/api'].headers).toBeUndefined();
        });
    });
});
